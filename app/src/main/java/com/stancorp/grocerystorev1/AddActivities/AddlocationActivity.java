package com.stancorp.grocerystorev1.AddActivities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;
import com.stancorp.grocerystorev1.AutoCompleteAdapter.AutoCompleteItemAdapter;
import com.stancorp.grocerystorev1.Classes.DeliveryAddress;
import com.stancorp.grocerystorev1.Classes.ItemStockInfo;
import com.stancorp.grocerystorev1.Classes.Location;
import com.stancorp.grocerystorev1.Classes.LocationStockItem;
import com.stancorp.grocerystorev1.Classes.maxindex;
import com.stancorp.grocerystorev1.GlobalClass.Gfunc;
import com.stancorp.grocerystorev1.R;
import com.stancorp.grocerystorev1.SmallRecyclerViewAdapter.CodesItemRecyclerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class AddlocationActivity extends AppCompatActivity {

    AutoCompleteTextView Itemsearch;
    Handler itemhandler = new Handler();
    ProgressBar AutoProgress;
    AutoCompleteItemAdapter itemAdapter;
    Button ItemAddButton;
    String hint;
    EditText ItemStock;
    String Shopcode;
    String LocationCode;
    FirebaseFirestore firebaseFirestore;
    RelativeLayout ProgressLayout;
    NestedScrollView scrollview;

    Gfunc gfunc;

    RecyclerView recyclerView;
    CodesItemRecyclerAdapter codesRecyclerAdapter;
    RecyclerView.LayoutManager mLayoutManager;

    ArrayList<EditText> editTexts;
    ArrayList<EditText> alertEditTexts;
    ArrayList<String> ItemString;
    Pair<String,Boolean> itemvalid;
    LinkedHashMap<String, ItemStockInfo> ItemCodes;
    LinkedHashMap<String, ItemStockInfo> items;
    LinkedHashMap<String, Float> StockValue;
    LinkedHashMap<String, LocationStockItem> locationStockItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addlocation);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("Add Location");
        gfunc = new Gfunc();
        ProgressLayout = findViewById(R.id.ProgressLayout);
        Shopcode = getIntent().getStringExtra("ShopCode");

        //Arraylists initialization
        editTexts = new ArrayList<>(Arrays.<EditText>asList((EditText) findViewById(R.id.AddLocationEditText2)
                , (EditText) findViewById(R.id.AddLocationEditText3), (EditText) findViewById(R.id.AddLocationEditText4),
                (EditText) findViewById(R.id.AddLocationEditText5),(EditText) findViewById(R.id.AddLocationEditText6)));
        ItemString = new ArrayList<>(Arrays.asList("Location Code", "Location Name", "State, Location is in"
                , "City, Location is in","Streey, Location is in", "Pincode of Location"));
        items = new LinkedHashMap<>();
        locationStockItems = new LinkedHashMap<>();
        StockValue = new LinkedHashMap<>();
        ItemCodes = new LinkedHashMap<>();

        Itemsearch = findViewById(R.id.ItemsearchAuto);
        AutoProgress = findViewById(R.id.autoprogress);
        Itemsearch.setThreshold(0);
        Itemsearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                itemhandler.removeCallbacksAndMessages(null);
            }

            @Override
            public void afterTextChanged(final Editable editable) {
                Itemsearch.dismissDropDown();
                if (editable.toString().length() > 0) {
                    AutoProgress.setVisibility(View.VISIBLE);
                    if (items.containsKey(editable.toString())) {
                        AutoProgress.setVisibility(View.GONE);
                        return;
                    }
                }
                else
                    AutoProgress.setVisibility(View.GONE);

                itemhandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (editable.toString().length() > 0) {
                            addItems(editable.toString());
                        }
                    }
                }, 1500);
            }
        });
        Itemsearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Itemsearch.showDropDown();
            }
        });
        itemAdapter = new AutoCompleteItemAdapter(getApplicationContext(), items);
        Itemsearch.setAdapter(itemAdapter);

        firebaseFirestore = FirebaseFirestore.getInstance();
        ItemAddButton = findViewById(R.id.ItemaddButton);
        scrollview = findViewById(R.id.AddLocationScrollView);
        scrollview.setEnabled(false);

        // Setting up Recycler View
        recyclerView = findViewById(R.id.ItemStockList);
        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        codesRecyclerAdapter = new CodesItemRecyclerAdapter(getApplicationContext(), locationStockItems, ItemCodes, StockValue);
        recyclerView.setAdapter(codesRecyclerAdapter);

        ItemAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AddItemOpeningStock();
            }
        });
    }

    private void addItems(final String search) {
        items.clear();
        String strFrontCode = "", strEndCode = "", startcode = "", endcode = "";
        int strlength = search.length();

        if (search.compareTo("") != 0) {
            strFrontCode = search.substring(0, strlength - 1);
            strEndCode = search.substring(strlength - 1, search.length());
            startcode = search;
            endcode = strFrontCode + Character.toString((char) (strEndCode.charAt(0) + 1));
        } else {
            startcode = "a";
            endcode = "{";
        }

        firebaseFirestore.collection(Shopcode).document("doc")
                .collection("ItemStockInfo").whereGreaterThanOrEqualTo("name", startcode)
                .whereLessThan("name", endcode).whereEqualTo("valid", true).limit(10).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                AutoProgress.setVisibility(View.GONE);
                if (!task.getResult().isEmpty()) {
                    for (DocumentSnapshot doc : task.getResult()) {
                        ItemStockInfo itemStockInfo = doc.toObject(ItemStockInfo.class);
                        items.put(itemStockInfo.ItemCode, itemStockInfo);
                    }
                    Itemsearch.showDropDown();
                    itemAdapter.updateList(items);
                } else {
                    if (search.compareTo("") == 0) {
                        Itemsearch.setText("No Item Added");
                        Itemsearch.setEnabled(false);
                        ItemAddButton.setEnabled(false);
                        ItemAddButton.setAlpha((float) 0.5);
                    } else {
                        Itemsearch.setError("No Result Found");
                    }
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {

        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

        }
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder alert = new AlertDialog.Builder(AddlocationActivity.this, R.style.MyDialogTheme);
        alert.setMessage("Discard Location");
        alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }).setNegativeButton("Cancel", null).show();
    }

    private void AddItemOpeningStock() {
        boolean flag = true;
        if(Itemsearch.getText().toString().length() == 0){
            flag = false;
            Itemsearch.requestFocus();
            Itemsearch.setError("Please enter an item");
        }
        if (ItemCodes.containsKey(Itemsearch.getText().toString())) {
            flag = false;
            Itemsearch.setText("");
            Itemsearch.requestFocus();
            Itemsearch.setError("Item Code already entered");
        }
        if (flag) {
            firebaseFirestore.collection(Shopcode).document("doc").collection("ItemStockInfo")
                    .document(Itemsearch.getText().toString()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.getResult().exists()) {
                        ItemStockInfo itemStockInfo = task.getResult().toObject(ItemStockInfo.class);
                        if (itemStockInfo.valid) {
                            AddItemStockAlert(itemStockInfo);
                        } else {
                            Itemsearch.setText("");
                            Itemsearch.requestFocus();
                            Itemsearch.setError("Item Code is not valid");
                        }
                    } else {
                        Itemsearch.setText("");
                        Itemsearch.requestFocus();
                        Itemsearch.setError("Item Code does not exist");
                    }
                }
            });
        }
    }

    private void AddItemStockAlert(final ItemStockInfo tempitem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(AddlocationActivity.this, R.style.AlertDialogTheme);
        View view = LayoutInflater.from(getApplicationContext()).inflate(
                R.layout.alertdialog_additem, (RelativeLayout) findViewById(R.id.additemalertcontainer)
        );
        builder.setView(view);
        final AlertDialog alertDialog = builder.create();
        view.findViewById(R.id.CancelAddItemButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });

        alertEditTexts = new ArrayList<EditText>(new ArrayList<EditText>(Arrays.<EditText>asList(
                (EditText) view.findViewById(R.id.AddItemOpeningStock), (EditText) view.findViewById(R.id.AddItemOpeningValue),
                (EditText) view.findViewById(R.id.AddItemReorderQty)
        )));

        for (int i = 0; i < alertEditTexts.size(); i++) {
            alertEditTexts.get(i).setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        }

        ItemStock = view.findViewById(R.id.AddItemOpeningStock);
        hint = ItemStock.getHint().toString();
        ItemStock.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);


        view.findViewById(R.id.ConfirmAddAdminButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Boolean flag = true;
                for (int i = 0; i < alertEditTexts.size(); i++) {
                    String text = alertEditTexts.get(i).getText().toString();
                    if (text.length() == 0 || gfunc.checkifcharexistsmorethanonce(text, '.')) {
                        alertEditTexts.get(i).setText("");
                        alertEditTexts.get(i).requestFocus();
                        alertEditTexts.get(i).setError("Enter Valid Value");
                        flag = false;
                        break;
                    }
                }
                if (flag) {
                    Float tempOpenValue = Float.parseFloat(alertEditTexts.get(1).getText().toString());
                    LocationStockItem tempLCI = new LocationStockItem(alertEditTexts.get(0).getText().toString()
                            , alertEditTexts.get(2).getText().toString(), true, tempitem.ItemCode, "");
                    locationStockItems.put(tempitem.ItemCode, tempLCI);
                    ItemCodes.put(tempitem.ItemCode, tempitem);
                    StockValue.put(tempitem.ItemCode, tempOpenValue);
                    codesRecyclerAdapter.notifyDataSetChanged();
                    alertDialog.dismiss();
                }

            }
        });
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }
        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.register_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.Register:
                AlertDialog.Builder alert = new AlertDialog.Builder(AddlocationActivity.this, R.style.MyDialogTheme);
                alert.setMessage("Confirm Location Details");
                alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        registerLocation();
                    }
                }).setNegativeButton("Cancel", null).show();
                return true;
        }
        return false;
    }

    private void registerLocation() {
        boolean flag = true;
        for (int i = 0; i < editTexts.size(); i++) {
            if (editTexts.get(i).getText().toString().length() == 0) {
                editTexts.get(i).setText("");
                editTexts.get(i).setError("Enter " + ItemString.get(i + 1));
                editTexts.get(i).requestFocus();
                flag = false;
                break;
            }
        }
        if (flag) {
            initiatetransaction();
        }
    }

    private void initiatetransaction() {
        SDProgress(true);
        String Name = editTexts.get(0).getText().toString().trim().toLowerCase();
        DeliveryAddress address = new DeliveryAddress(editTexts.get(3).getText().toString().trim().toLowerCase(),editTexts.get(2).getText().toString().trim().toLowerCase()
                ,editTexts.get(1).getText().toString().trim().toLowerCase(),Long.parseLong(editTexts.get(4).getText().toString().trim().toLowerCase()));
        final Location location = new Location("", Name, address, 0);
        firebaseFirestore.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                ArrayList<ItemStockInfo> newitemstocks = new ArrayList<>();
                for (int i = 0; i < ItemCodes.size(); i++) {
                    ItemStockInfo tempstockinfo = new ItemStockInfo((ItemStockInfo) ItemCodes.values().toArray()[i]);
                    tempstockinfo = transaction.get(firebaseFirestore.collection(Shopcode).document("doc")
                            .collection("ItemStockInfo").document(tempstockinfo.ItemCode)).toObject(ItemStockInfo.class);
                    if(!tempstockinfo.valid){
                        itemvalid = new Pair<>(tempstockinfo.ItemCode,true);
                        return null;
                    }
                    LocationStockItem locationStockItem = locationStockItems.get(tempstockinfo.ItemCode);
                    Float T_Bqty = Float.parseFloat(tempstockinfo.Total_Balance_Quantity) +
                            Float.parseFloat(locationStockItem.Balance_Qty);
                    Float T_price = Float.parseFloat(tempstockinfo.Total_Price) +
                            (Float.parseFloat(locationStockItem.Balance_Qty) * StockValue.get(tempstockinfo.ItemCode));
                    tempstockinfo.Total_Balance_Quantity = String.valueOf(T_Bqty);
                    tempstockinfo.Total_Price = String.valueOf(T_price);
                    newitemstocks.add(tempstockinfo);
                }
                if (transaction.get(firebaseFirestore.collection(Shopcode).document("maxIndex")).exists()) {
                    maxindex max = (maxindex) transaction.get(firebaseFirestore.collection(Shopcode).document("maxIndex"))
                            .toObject(maxindex.class);
                    location.code = "LOC-" + String.valueOf(max.locationCode + 1);
                    LocationCode = location.code;
                    location.codeno = max.locationCode + 1;
                    max.locationCode = max.locationCode + 1;
                    transaction.set(firebaseFirestore.collection(Shopcode).document("maxIndex"), max);
                } else {
                    location.code = "LOC-1";
                    LocationCode = location.code;
                    location.codeno = 1L;
                    maxindex max = new maxindex(0, 1, 0, 0, 0, 0);
                    transaction.set(firebaseFirestore.collection(Shopcode).document("maxIndex"), max);
                }
                DocumentReference doc = firebaseFirestore.collection(Shopcode).document("doc").collection("LocationDetails")
                        .document(location.code);
                transaction.set(doc, location);
                for (int i = 0; i < ItemCodes.size(); i++) {
                    ItemStockInfo tempstockinfo = new ItemStockInfo(newitemstocks.get(i));
                    LocationStockItem locationStockItem = locationStockItems.get(tempstockinfo.ItemCode);
                    locationStockItem.LocationCode = location.code;
                    transaction.set(firebaseFirestore.collection(Shopcode).document("doc").collection("Location")
                            .document(tempstockinfo.ItemCode+location.code), locationStockItem);
                    transaction.set(firebaseFirestore.collection(Shopcode).document("doc").collection("ItemStockInfo")
                            .document(tempstockinfo.ItemCode), tempstockinfo);
                }
                return null;
            }
        }).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    if(itemvalid !=null){
                        locationStockItems.remove(itemvalid.first);
                        ItemCodes.remove(itemvalid.first);
                        StockValue.remove(itemvalid.first);
                        codesRecyclerAdapter.notifyDataSetChanged();
                        Toast.makeText(getApplicationContext(),"Item " + itemvalid.first + " was made invalid during after addition into list",Toast.LENGTH_SHORT).show();
                    }
                    if(itemvalid == null){
                        Toast.makeText(getApplicationContext(), "Location entered", Toast.LENGTH_SHORT).show();
                        SDProgress(false);
                        showLocationCode();
                    }else{
                        itemvalid = null;
                        SDProgress(false);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "failed to enter location", Toast.LENGTH_SHORT).show();
                    SDProgress(false);
                    finish();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("failfirestore", e.getMessage());
            }
        });

    }

    private void showLocationCode() {
        AlertDialog.Builder alert = new AlertDialog.Builder(AddlocationActivity.this, R.style.MyDialogTheme);
        alert.setMessage("Location Code assigned " + LocationCode);
        alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        }).setCancelable(false).show();
    }

    public void SDProgress(boolean show) {
        if (show) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            ProgressLayout.setVisibility(View.VISIBLE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            ProgressLayout.setVisibility(View.GONE);
        }
    }
}