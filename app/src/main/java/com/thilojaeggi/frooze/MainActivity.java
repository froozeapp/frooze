package com.thilojaeggi.frooze;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.*;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.androidstudy.networkmanager.Monitor;
import com.androidstudy.networkmanager.Tovuti;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.cloudinary.android.MediaManager;
import com.danikula.videocache.HttpProxyCacheServer;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import im.ene.toro.exoplayer.Config;

public class MainActivity  extends AppCompatActivity implements BillingProcessor.IBillingHandler {
    Context mContext = this;
    private BillingProcessor bp;
    private int startingPosition;
    private int newPosition;
    SharedPreferences prefs;
    Integer selection;
    BottomNavigationView bottomNav;
    ImageButton uploadvideobutton;
    View notificationBadge;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fresco.initialize(this);
        AudienceNetworkAds.initialize(this);
        setContentView(R.layout.activity_main);
        uploadvideobutton = findViewById(R.id.upload);
        RateThisApp.onCreate(this);
        Intent notifications = new Intent(this, NotificationService.class);
        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            startService(notifications);
        } else {
            Intent gotologin = new Intent(MainActivity.this, LoginActivity.class);
            gotologin.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(gotologin);
        }

        RateThisApp.Config rconfig = new RateThisApp.Config();
        rconfig.setTitle(R.string.rate_dialog_title);
        rconfig.setMessage(R.string.rate_dialog_message);
        rconfig.setYesButtonText(R.string.rate_dialog_action_rate);
        rconfig.setNoButtonText(R.string.rate_dialog_action_never);
        rconfig.setCancelButtonText(R.string.rate_dialog_action_later);
        RateThisApp.init(rconfig);
        RateThisApp.showRateDialogIfNeeded(this);
        prefs = getSharedPreferences("PREFS", MODE_PRIVATE);
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink = null;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                            Intent intent = new Intent(MainActivity.this, PostFromLink.class);
                            intent.putExtra("postid", deepLink.toString());
                            startActivity(intent);
                        }


                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("DynamicLinks", "getDynamicLink:onFailure", e);
                    }
                });
        bp = new BillingProcessor(getApplicationContext(), "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtF6vzGWT3jyRKdkNagWdw5CaW4TvPYEflDTAQzDr3f/rxFqpilu9jGLCjnJ3HNfMbtrWqgttc7yHOpuV/AMzOF61n+yhQRfHEwysGSxsXklccZ0OxHEXzcWz1MEtjesvbf9s1P/cGevKEwtsEQiM/fl4wemUbowNmVDIhd71xQnGzNuJ7J+hMyj/1VmXhebTaaKyd9TwnEbO1DH9/eLLntaruWwHqD02XsAqmTyi+PVNUluM0ZJbamXk3+vsvxwABdPxfofYAduHe/9JHp4q8YrBQQZmApt+g1dOyTRI50gvse7koL9KbTdCWKfJbT9MdxLUQ+HJvRauWmD+URXs/wIDAQAB", this);
        bp.initialize();
        bp.loadOwnedPurchasesFromGoogle();
        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(navListener);
        bottomNav.setSelectedItemId(R.id.nav_home);
        uploadvideobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PostActivity.class);
                startActivity(intent);
            }
        });
        if (savedInstanceState == null) {
            loadFragment(new TrendingPostsFragment(), 1);

        }
        Tovuti.from(this).monitor(new Monitor.ConnectivityListener(){
            @Override
            public void onConnectivityChanged(int connectionType, boolean isConnected, boolean isFast){
                if (!isConnected){
                    Intent intent = new Intent(MainActivity.this, NoInternetActivity.class);
                    startActivity(intent);
                }
            }
        });
        addBadgeView();
        boolean hasnotifs = prefs.getBoolean("hasnotifs", false);
        if (hasnotifs){
            notificationBadge.setVisibility(View.VISIBLE);
        } else {
            notificationBadge.setVisibility(View.GONE);
        }
    }



    private BottomNavigationView.OnNavigationItemSelectedListener navListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()) {
                        case R.id.nav_home:
                            selection = prefs.getInt("selectdefaulttab", 1);
                            switch (selection){
                                case 0:
                                    selectedFragment = new NewPostsFragment();
                                    break;
                                case 1:
                                    selectedFragment = new TrendingPostsFragment();
                                    break;
                                case 2:
                                    selectedFragment = new FollowPostsFragment();
                                    break;
                            }
                            newPosition = 1;
                            break;
                        case R.id.nav_search:
                            selectedFragment = new SearchFragment();
                            newPosition = 2;
                            break;

                        case R.id.nav_notifications:
                            selectedFragment = new NotificationFragment();
                            newPosition = 3;
                            break;

                        case R.id.nav_profile:
                            selectedFragment = new ProfileFragment();
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            SharedPreferences.Editor editor = mContext.getSharedPreferences("PREFS", Context.MODE_PRIVATE).edit();
                            editor.putString("profileid", user.getUid());
                            editor.apply();
                            newPosition = 4;
                            break;
                    }
                    return loadFragment(selectedFragment, newPosition);
                }
            };
    private boolean loadFragment(Fragment fragment, int newPosition) {
        if(fragment != null) {
            if(startingPosition > newPosition) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right );
                transaction.replace(R.id.fragment_container, fragment);
                transaction.commit();
            }
            if(startingPosition < newPosition) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
                transaction.replace(R.id.fragment_container, fragment);
                transaction.commit();
            }
            startingPosition = newPosition;
            return true;
        }

        return false;
    }
    private void addBadgeView() {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) bottomNav.getChildAt(0);
        BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(3);
        notificationBadge = LayoutInflater.from(this).inflate(R.layout.notification_badge, menuView, false);
        itemView.addView(notificationBadge);

    }
    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {

    }

    @Override
    public void onPurchaseHistoryRestored() {

    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {

    }

    @Override
    public void onBillingInitialized() {
    /*    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {

        boolean purchaseResult = bp.loadOwnedPurchasesFromGoogle();
        if(purchaseResult){
            TransactionDetails subscriptionTransactionDetails = bp.getSubscriptionTransactionDetails("premiumonemonth");
            if(subscriptionTransactionDetails!=null) {
                //User is still subscribed
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                reference.child("premium").setValue("true");
            } else {
                //Not subscribed
                DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                reference.child("premium").setValue("false");
            }
        }else{
            Log.d("BILLING", "loadOwnedPurchasesFromGoogle returned false");
        }
        }*/
    }

}