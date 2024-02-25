package com.example.nebula_attendance_tracking;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Executor;


public class Nebula_dashboard extends AppCompatActivity {
    // start copying code from this line till....... line no. : 300 and also see line no : 161
    private boolean showDialog = true;
    private static final int REQUEST_CODE = 5;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private static final String TAG = "Biometric";
    private static final String HASH_KEY_ALIAS = "biometric_hash_key";
    private String hashKey = null;
    private String Stored_hash_key;
    DatabaseReference reference;
    public static final String SHARED_PREFS = "shared_prefs";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nebula_dashboard);

        reference = FirebaseDatabase.getInstance().getReference();

        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(Nebula_dashboard.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(),
                                "" , Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(Nebula_dashboard.this, "Authentication succeeded.", Toast.LENGTH_SHORT).show();

            }
            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });
//        TODO-> just add your qr button in this code,after adding the button use clicklistener and call this
//          two method .
//            checkSourceActivity();
//            checkBiometricAuthentication();
//              below is the example of following such type
        Button QR = findViewById(R.id.qrscan);
        QR.setOnClickListener(v ->{
            checkSourceActivity();
            checkBiometricAuthentication();
                });
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Nebula")
                .setSubtitle("Authenticate Yourself")
                .setNegativeButtonText("Cancel")
                .build();
    }
    public static class SHAEncoding {
        public byte[] obtainSHA(String s) throws NoSuchAlgorithmException {
            MessageDigest msgDgst = MessageDigest.getInstance("SHA-512");
            return msgDgst.digest(s.getBytes(StandardCharsets.UTF_8));
        }

        public String toHexStr(byte[] hash) {
            BigInteger no = new BigInteger(1, hash);
            StringBuilder hexStr = new StringBuilder(no.toString(16));
            while (hexStr.length() < 32) hexStr.insert(0, '0');
            return hexStr.toString();
        }
    }
    private String generateHashKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (!keyStore.containsAlias(HASH_KEY_ALIAS)) {
                KeyGenParameterSpec keyGenParameterSpec = new KeyGenParameterSpec.Builder(HASH_KEY_ALIAS,
                        KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .setUserAuthenticationRequired(true)
                        .build();
                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                keyPairGenerator.initialize(keyGenParameterSpec);
                keyPairGenerator.generateKeyPair();
            }

            // Generate hash key
            byte[] hash = generateHash("Your secret data".getBytes(StandardCharsets.UTF_8));
            hashKey = Base64.encodeToString(hash, Base64.DEFAULT);
            Log.d(TAG, "Hash key generated: " + hashKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hashKey;
    }

    private byte[] generateHash(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input);
    }
    private void write_user_authentication_info(String Designation, String sanitizedEmail, String instituteID, String Authentication_Key) {
        // Create a reference to the "Personal Information" node for the specific user
        DatabaseReference personalInfoRef = reference.child("Institute").child(instituteID)
                .child(Designation).child(sanitizedEmail).child("Personal Information");

        // Retrieve existing data from the database
        personalInfoRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // If dataSnapshot exists and has children
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    // Get the current data
                    Map<String, Object> currentData = (Map<String, Object>) dataSnapshot.getValue();

                    // Create a Post object with the provided data
                    PostEncodedHashkey post = new PostEncodedHashkey( sanitizedEmail, instituteID, Authentication_Key);

                    // Convert the Post object to a map
                    Map<String, Object> newPostValue = post.toMap();

                    // Append the new data to the existing data
                    currentData.putAll(newPostValue);

                    // Update the "Personal Information" node with the updated data
                    personalInfoRef.setValue(currentData);
                } else {
                    // If no existing data, simply set the new data
                    PostEncodedHashkey post = new PostEncodedHashkey( sanitizedEmail, instituteID,Authentication_Key);

                    // Convert the Post object to a map
                    Map<String, Object> newPostValue = post.toMap();

                    // Update the "Personal Information" node with the new data
                    personalInfoRef.setValue(newPostValue);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
                Log.e(TAG, "Error appending data to Personal Information: " + databaseError.getMessage());
                Toast.makeText(Nebula_dashboard.this, "Error appending data to Personal Information: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void checkSourceActivity() {
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("source_activity")) {
            String sourceActivity = intent.getStringExtra("source_activity");
            if (sourceActivity.equals("Nebula_personal_information")) {
                // Show warning dialog for Nebula_personal_information
                showDialog = true;
                showWarningDialog();
            } else {
                showDialog = false;
                checkBiometricAuthentication();
            }
        }
    }

    private void showWarningDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(Nebula_dashboard.this, R.style.CustomAlertDialogTheme);
        builder.setTitle("Warning")
                .setMessage("The fingerprint you are using will be the final one for logging into the app in the future.\nIt cannot be changed until the admin permits altering the fingerprint impression.")
                .setPositiveButton("Got it", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (showDialog) {
                            checkBiometricAuthentication();
                        }
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }
    private void checkBiometricAuthentication() {
        if (showDialog) {
            biometricPrompt.authenticate(promptInfo);
        }
    }
//    Intent intent = getIntent();
//                if (intent != null && intent.hasExtra("source_activity")) {
//        String sourceActivity = intent.getStringExtra("source_activity");
//        if (sourceActivity.equals("Nebula_personal_information")) {
//            String encoded_hashkey,app_encoded_hashkey;
//            Stored_hash_key = generateHashKey();
//            SHAEncoding sha = new SHAEncoding();
//            try { //TODO: Convert str to encrypted-str >>>
//                SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFS,MODE_PRIVATE);
//                String college_id = sharedPref.getString("Institute_id","");
//                String Designation = sharedPref.getString("Designation","");
//                String Email = sharedPref.getString("Email","");
//                String sanitizedEmail = Email.replace(".", "_")
//                        .replace("#", "_")
//                        .replace("$", "_")
//                        .replace("[", "_")
//                        .replace("]", "_");
//                encoded_hashkey = sha.toHexStr(sha.obtainSHA(Stored_hash_key));
//                SharedPreferences.Editor editor = sharedPref.edit();
//                editor.putString("encoded_hashkey", encoded_hashkey);
//                editor.apply();
//                write_user_authentication_info(Designation,sanitizedEmail,college_id,encoded_hashkey);
//                Toast.makeText(getApplicationContext(),
//                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException(e);
//            }
//        } else if (sourceActivity.equals("Nebula_login")) {
//            // Handle checking the hashkey for Nebula_login
//            SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
//            String storedEncodedHashkey = sharedPref.getString("encoded_hashkey", "");
//            String generatedHashkey = generateHashKey();
//            SHAEncoding sha = new SHAEncoding();
//            String generatedEncodedHashkey = null;
//            try {
//                generatedEncodedHashkey = sha.toHexStr(sha.obtainSHA(generatedHashkey));
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException(e);
//            }
//            if (storedEncodedHashkey.equals(generatedEncodedHashkey)) {
//                // Hashkeys match, provide access to Nebula_dashboard
//                Toast.makeText(getApplicationContext(),
//                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
//                Intent nebulaDashboardIntent = new Intent(Nebula_dashboard.this, Nebula_dashboard.class);
//                startActivity(nebulaDashboardIntent);
//                finish(); // Finish login activity to prevent user from going back
//            } else {
//                // Hashkeys don't match, show authentication failed message
//                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
//                // Increment authentication attempt counter and handle lockout logic if needed
//            }
//        }
//        else {
//            // Handle checking the hashkey for Nebula_login
//            SharedPreferences sharedPref = getSharedPreferences(SHARED_PREFS, MODE_PRIVATE);
//            String storedEncodedHashkey = sharedPref.getString("encoded_hashkey", "");
//            String generatedHashkey = generateHashKey();
//            SHAEncoding sha = new SHAEncoding();
//            String generatedEncodedHashkey = null;
//            try {
//                generatedEncodedHashkey = sha.toHexStr(sha.obtainSHA(generatedHashkey));
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException(e);
//            }
//
//            if (storedEncodedHashkey.equals(generatedEncodedHashkey)) {
//                // Hashkeys match, provide access to Nebula_dashboard
//                Toast.makeText(getApplicationContext(),
//                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
//                Intent nebulaDashboardIntent = new Intent(Nebula_dashboard.this, Nebula_dashboard.class);
//                startActivity(nebulaDashboardIntent);
//                finish(); // Finish login activity to prevent user from going back
//            } else {
//                // Hashkeys don't match, show authentication failed message
//                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
//                // Increment authentication attempt counter and handle lockout logic if needed
//            }
//        }
//    }

}