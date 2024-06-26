package com.example.tkfinalproject.DB;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.example.tkfinalproject.RePostry.User;
import com.example.tkfinalproject.Utility.BaseActivity;
import com.example.tkfinalproject.Utility.ConnectivityListener;
import com.example.tkfinalproject.Utility.IonComplete;
import com.example.tkfinalproject.Utility.UtilityClass;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyFireBaseHelper {
    FirebaseDatabase database;
    DatabaseReference reference;
    Context myContext;
    UtilityClass utilityClass;
    ExecutorService executorService;
    ConnectivityListener connectivityListener;
    AtomicBoolean isConnected = new AtomicBoolean(true); // Track connectivity status
    private Future<?> currentTask; // Track the current task

    public MyFireBaseHelper(Context context, LifecycleOwner lifecycleOwner) {
        utilityClass = new UtilityClass(context);
        try {
            executorService = Executors.newSingleThreadExecutor();
            connectivityListener = new ConnectivityListener(context);
            database = FirebaseDatabase.getInstance();
            reference = database.getReference("Users");
            myContext = context;

            // Observe connectivity changes
            connectivityListener.observe(lifecycleOwner, new Observer<Boolean>() {
                @Override
                public void onChanged(Boolean connected) {
                    if (!connected) {
                        stopCurrentTask();
                    }
                    isConnected.set(connected);
                }
            });
        } catch (Exception e) {
            utilityClass.showAlertExp();
        }
    }

    // Method to stop the current task
    private void stopCurrentTask() {
        if (currentTask != null) {
            executorService.shutdownNow();
            utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
            if (myContext instanceof BaseActivity){
                ((BaseActivity) myContext).hideLoadingOverlay();
            }
        }
    }

    public void addUser(User user, IonComplete ionComplete) {
        if (!isConnected.get()) {
            utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
            ionComplete.onCompleteBool(false);
            return;
        }
        currentTask = executorService.submit(() -> {
            try {
                reference.child(user.getUsername()).setValue(user).addOnCompleteListener(task -> {
                    if (!isConnected.get()) {
                        utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
                        ionComplete.onCompleteBool(false);
                        return;
                    }
                    ionComplete.onCompleteBool(task.isSuccessful());
                });
            } catch (Exception e) {
                utilityClass.showAlertExp();
                ionComplete.onCompleteBool(false);
            }
        });
    }

    public void update(User user, IonComplete ionComplete) {
        if (!isConnected.get()) {
            utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
            ionComplete.onCompleteBool(false);
            return;
        }
        currentTask = executorService.submit(() -> {
            try {
                Map<String, Object> updates = new HashMap<>();
                updates.put("username", user.getUsername());
                updates.put("pass", user.getPass());
                reference.child(user.getUsername()).updateChildren(updates).addOnCompleteListener(task -> {
                    if (!isConnected.get()) {
                        utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
                        ionComplete.onCompleteBool(false);
                        return;
                    }
                    ionComplete.onCompleteBool(task.isSuccessful());
                });
            } catch (Exception e) {
                utilityClass.showAlertExp();
                ionComplete.onCompleteBool(false);
            }
        });
    }

    public interface checkUser {
        void onCheckedUser(boolean flag);
    }

    public void userNameExsIts(String username, checkUser checkUser) {
        if (!isConnected.get()) {
            utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
            checkUser.onCheckedUser(false);
            return;
        }
        currentTask = executorService.submit(() -> {
            try {
                reference.child(username).get().addOnCompleteListener(task -> {
                    if (!isConnected.get()) {
                        utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
                        checkUser.onCheckedUser(false);
                        return;
                    }
                    DataSnapshot dataSnapshot = task.getResult();
                    checkUser.onCheckedUser(task.isSuccessful() && String.valueOf(dataSnapshot.child("username").getValue()).equals(username));
                });
            } catch (Exception e) {
                utilityClass.showAlertExp();
                checkUser.onCheckedUser(false);
            }
        });
    }

    public void userExsits(User user, checkUser checkUser) {
        if (!isConnected.get()) {
            utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
            checkUser.onCheckedUser(false);
            return;
        }
        currentTask = executorService.submit(() -> {
            try {
                reference.child(user.getUsername()).get().addOnCompleteListener(task -> {
                    if (!isConnected.get()) {
                        utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
                        checkUser.onCheckedUser(false);
                        return;
                    }
                    DataSnapshot dataSnapshot = task.getResult();
                    if (task.isSuccessful() && String.valueOf(dataSnapshot.child("username").getValue()).equals(user.getUsername())) {
                        checkUser.onCheckedUser(String.valueOf(dataSnapshot.child("pass").getValue()).equals(user.getPass()));
                    } else {
                        checkUser.onCheckedUser(false);
                    }
                });
            } catch (Exception e) {
                utilityClass.showAlertExp();
                checkUser.onCheckedUser(false);
            }
        });
    }

    public void getUserByName(String username, IonComplete.IonCompleteUser user) {
        if (!isConnected.get()) {
            utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
            user.onCompleteUser(null);
            return;
        }
        currentTask = executorService.submit(() -> {
            try {
                reference.child(username).get().addOnCompleteListener(task -> {
                    if (!isConnected.get()) {
                        utilityClass.showAlertInternet(); // Changed from showAlertExp to showAlertInternet
                        user.onCompleteUser(null);
                        return;
                    }
                    if (task.isSuccessful() && task.getResult() != null) {
                        DataSnapshot dataSnapshot = task.getResult();
                        String retrievedUsername = String.valueOf(dataSnapshot.child("username").getValue());
                        if (username.equals(retrievedUsername)) {
                            String pass = String.valueOf(dataSnapshot.child("pass").getValue());
                            user.onCompleteUser(new User(retrievedUsername, pass));
                        } else {
                            user.onCompleteUser(null);
                        }
                    } else {
                        user.onCompleteUser(null);
                    }
                });
            } catch (Exception e) {
                utilityClass.showAlertExp();
                user.onCompleteUser(null);
            }
        });
    }

    // Method to destroy the MyFireBaseHelper instance
    public void destroy() {
        connectivityListener.stopObserving();
        executorService.shutdown();
    }



//    public boolean userNameExist(String userName){
//        User user = getUserByName(userName)
//        return user != null && user.getUsername().equals(userName);
//    }
//    public boolean checkUserExistence(User user , checkUser) {
//            @Override
//            public void onCheckedUser(User user) {
//
//            }
//        });
//        boolean b = user1 != null;
//        boolean x = user1.getUsername().equals(user.getUsername());
//        boolean u =  user1.getPass().equals(user.getPass());
//        return user1 != null && user1.getUsername().equals(user.getUsername()) && user1.getPass().equals(user.getPass());
//    }
//    private User getUserByName(String username, checkUser callback){
//        reference.child(username).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DataSnapshot> task) {
//                if (!task.isSuccessful()) {
//                    callback.onCheckedUser(null);
//                }
//                else {
//                    callback.onCheckedUser(task.getResult().getValue(User.class));
//                }
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//
//            }
//        });
//        return null;
//    }
//    private ArrayList<User> getUsers(){
//        ArrayList<User> list = new ArrayList<>();
//        reference.child("users").child(userId).get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
//            @Override
//            public void onComplete(@NonNull Task<DataSnapshot> task) {
//                if (!task.isSuccessful()) {
//                    Log.e("firebase", "Error getting data", task.getException());
//                }
//                else {
//                    Log.d("firebase", String.valueOf(task.getResult().getValue()));
//                }
//            }
//        });
//        return  list;
//    }

//    public boolean isExsist(User user){
//        reference.child("Users").orderByChild("username").addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//
//            }
//        })
//    }

}