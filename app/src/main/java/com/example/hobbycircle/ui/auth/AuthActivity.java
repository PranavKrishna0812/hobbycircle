package com.example.hobbycircle.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.hobbycircle.R;
import com.example.hobbycircle.ui.home.HomeActivity;
import com.example.hobbycircle.data.repository.UserRepository;
import com.example.hobbycircle.utils.Constants;
import com.example.hobbycircle.utils.PreferenceManager;
import com.example.hobbycircle.utils.UserRoleUtil;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private TextInputLayout tilName;
    private TextInputLayout tilEmail;
    private TextInputLayout tilPassword;
    private TextInputLayout tilConfirmPassword;

    private EditText etName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;

    private Button btnSignIn;
    private Button btnGoogle;
    private TextView tvToggleAuth;
    private TextView tvAuthTitle;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private PreferenceManager preferenceManager;
    private UserRepository userRepository;
    private GoogleSignInClient googleSignInClient;

    private boolean isRegisterMode = false;

    private final ActivityResultLauncher<Intent> googleSignInLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result == null || result.getData() == null) {
                    showToast("Google sign-in cancelled.");
                    return;
                }
                try {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null || account.getIdToken() == null) {
                        showToast("Unable to get Google token.");
                        return;
                    }
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    showToast("Google sign-in failed (Status Code " + e.getStatusCode() + "): " + safe(e.getMessage()));
                } catch (Exception e) {
                    showToast("Unexpected error: " + safe(e.getMessage()));
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        printSignatureSha1();

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        userRepository = new UserRepository(this);

        FirebaseUser current = firebaseAuth.getCurrentUser();
        if (current != null) {
            loadRoleAndNavigate(current);
            return;
        }

        setContentView(R.layout.activity_auth);
        initViews();
        setupGoogleSignIn();
        setupClicks();
        updateAuthModeUi();
    }

    private void printSignatureSha1() {
        try {
            android.content.pm.PackageInfo info = getPackageManager().getPackageInfo(
                    getPackageName(),
                    android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature signature : info.signatures) {
                // SHA-1
                java.security.MessageDigest md1 = java.security.MessageDigest.getInstance("SHA-1");
                md1.update(signature.toByteArray());
                byte[] digest1 = md1.digest();
                StringBuilder toPrint1 = new StringBuilder();
                for (int i = 0; i < digest1.length; i++) {
                    if (i > 0) toPrint1.append(":");
                    int b = digest1[i] & 0xff;
                    String hex = Integer.toHexString(b).toUpperCase();
                    if (hex.length() == 1) toPrint1.append("0");
                    toPrint1.append(hex);
                }

                // SHA-256
                java.security.MessageDigest md256 = java.security.MessageDigest.getInstance("SHA-256");
                md256.update(signature.toByteArray());
                byte[] digest256 = md256.digest();
                StringBuilder toPrint256 = new StringBuilder();
                for (int i = 0; i < digest256.length; i++) {
                    if (i > 0) toPrint256.append(":");
                    int b = digest256[i] & 0xff;
                    String hex = Integer.toHexString(b).toUpperCase();
                    if (hex.length() == 1) toPrint256.append("0");
                    toPrint256.append(hex);
                }

                android.util.Log.d("HobbyCircleSHA1", "=================================================");
                android.util.Log.d("HobbyCircleSHA1", "ACTUAL RUNNING SIGNING SHA-1: " + toPrint1.toString());
                android.util.Log.d("HobbyCircleSHA1", "ACTUAL RUNNING SIGNING SHA-256: " + toPrint256.toString());
                android.util.Log.d("HobbyCircleSHA1", "CURRENT CLIENT ID IN APP: " + getString(R.string.default_web_client_id));
                android.util.Log.d("HobbyCircleSHA1", "=================================================");
            }
        } catch (Exception e) {
            android.util.Log.e("HobbyCircleSHA1", "Failed to get signature SHA-1", e);
        }
    }

    private void initViews() {
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvToggleAuth = findViewById(R.id.tvToggleAuth);
        tvAuthTitle = findViewById(R.id.tvAuthTitle);
    }

    private void setupGoogleSignIn() {
        String webClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClicks() {
        btnSignIn.setOnClickListener(v -> handleAuthSubmit());
        btnGoogle.setOnClickListener(v -> startGoogleSignIn());
    }

    private void updateAuthModeUi() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        if (isRegisterMode) {
            tvAuthTitle.setText("Create Account");
            tilName.setVisibility(View.VISIBLE);
            tilConfirmPassword.setVisibility(View.VISIBLE);
            btnSignIn.setText("Create Account");
        } else {
            tvAuthTitle.setText("Welcome Back");
            tilName.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);
            btnSignIn.setText("Sign In");
        }

        setupToggleAuthText();
    }

    private void setupToggleAuthText() {
        String fullText;
        String clickablePart;
        if (isRegisterMode) {
            fullText = "Already have an account? Sign In";
            clickablePart = "Sign In";
        } else {
            fullText = "Don't have an account? Register";
            clickablePart = "Register";
        }

        android.text.SpannableString spannable = new android.text.SpannableString(fullText);
        int start = fullText.indexOf(clickablePart);
        int end = start + clickablePart.length();

        android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
            @Override
            public void onClick(@NonNull android.view.View widget) {
                isRegisterMode = !isRegisterMode;
                updateAuthModeUi();
            }

            @Override
            public void updateDrawState(@NonNull android.text.TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getResources().getColor(R.color.hc_accent));
                ds.setUnderlineText(false);
                ds.setFakeBoldText(true);
            }
        };

        spannable.setSpan(clickableSpan, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvToggleAuth.setText(spannable);
        tvToggleAuth.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void handleAuthSubmit() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String email = safe(textOf(etEmail));
        String password = safe(textOf(etPassword));

        boolean hasError = false;

        if (!isValidEmail(email)) {
            tilEmail.setError("Enter a valid email address");
            hasError = true;
        }

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            hasError = true;
        }

        if (isRegisterMode) {
            String name = safe(textOf(etName));
            String confirm = safe(textOf(etConfirmPassword));

            if (name.isEmpty()) {
                tilName.setError("Full name is required");
                hasError = true;
            }

            if (!password.equals(confirm)) {
                tilConfirmPassword.setError("Passwords do not match");
                hasError = true;
            }

            if (!hasError) {
                registerWithEmailPassword(name, email, password);
            }
        } else {
            if (!hasError) {
                loginWithEmailPassword(email, password);
            }
        }
    }

    private void registerWithEmailPassword(String name, String email, String password) {
        setButtonsEnabled(false);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setButtonsEnabled(true);
                        showToast("Registration failed: " + safe(task.getException() != null ? task.getException().getMessage() : ""));
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        setButtonsEnabled(true);
                        showToast("Registration succeeded, but user is null.");
                        return;
                    }

                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

                    user.updateProfile(profileUpdates)
                            .addOnCompleteListener(task1 -> {
                                saveUserDocument(user, "email_password", () -> {
                                    setButtonsEnabled(true);
                                    loadRoleAndNavigate(user);
                                }, error -> {
                                    setButtonsEnabled(true);
                                    showToast(error);
                                });
                            });
                });
    }

    private void loginWithEmailPassword(String email, String password) {
        setButtonsEnabled(false);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setButtonsEnabled(true);

                    if (!task.isSuccessful()) {
                        showToast("Login failed: " + safe(task.getException() != null ? task.getException().getMessage() : ""));
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        showToast("Login succeeded, but user is null.");
                        return;
                    }

                    saveUserDocument(user, "email_password", () -> loadRoleAndNavigate(user), this::showToast);
                });
    }

    private void startGoogleSignIn() {
        try {
            setButtonsEnabled(false);
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                setButtonsEnabled(true);
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleSignInLauncher.launch(signInIntent);
            });
        } catch (Exception e) {
            setButtonsEnabled(true);
            showToast("Unable to start Google sign-in: " + safe(e.getMessage()));
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        setButtonsEnabled(false);

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setButtonsEnabled(true);

                    if (!task.isSuccessful()) {
                        showToast("Google authentication failed: " + safe(task.getException() != null ? task.getException().getMessage() : ""));
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        showToast("Google login succeeded, but user is null.");
                        return;
                    }

                    saveUserDocument(user, "google", () -> loadRoleAndNavigate(user), this::showToast);
                });
    }

    private void loadRoleAndNavigate(FirebaseUser user) {
        saveUserToPrefs(user);
        String uid = safe(user.getUid());
        if (uid.isEmpty()) {
            preferenceManager.saveUserRole(Constants.ROLE_USER);
            navigateToHome();
            return;
        }

        userRepository.fetchFirestoreRole(uid, new UserRepository.ResultCallback<String>() {
            @Override
            public void onSuccess(String firestoreRole) {
                String role = userRepository.resolveAndPersistRole(user, firestoreRole);
                if (!role.equals(UserRoleUtil.normalizeRole(firestoreRole))) {
                    userRepository.persistAuthUserDocument(user, "sync", role, new UserRepository.ResultCallback<Void>() {
                        @Override
                        public void onSuccess(Void data) {
                            navigateToHome();
                        }

                        @Override
                        public void onError(String message) {
                            navigateToHome();
                        }
                    });
                } else {
                    navigateToHome();
                }
            }

            @Override
            public void onError(String message) {
                userRepository.resolveAndPersistRole(user, null);
                navigateToHome();
            }
        });
    }

    private void saveUserDocument(FirebaseUser user, String provider, Runnable onSuccess, ErrorCallback errorCallback) {
        String role = UserRoleUtil.resolveRole(this, safe(user.getEmail()), null);
        userRepository.persistAuthUserDocument(user, provider, role, new UserRepository.ResultCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                preferenceManager.saveUserRole(role);
                onSuccess.run();
            }

            @Override
            public void onError(String message) {
                errorCallback.onError(message);
            }
        });
    }

    private void saveUserToPrefs(FirebaseUser user) {
        preferenceManager.saveUserId(safe(user.getUid()));
        preferenceManager.saveUserEmail(safe(user.getEmail()));

        String name = safe(user.getDisplayName());
        if (name.isEmpty()) {
            String email = safe(user.getEmail());
            name = email.contains("@") ? email.substring(0, email.indexOf("@")) : "User";
        }
        preferenceManager.saveUserName(name);
    }

    private void navigateToHome() {
        Intent intent = new Intent(AuthActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private String textOf(EditText editText) {
        return editText.getText() != null ? editText.getText().toString() : "";
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void setButtonsEnabled(boolean enabled) {
        btnSignIn.setEnabled(enabled);
        btnGoogle.setEnabled(enabled);
    }

    private void showToast(String message) {
        String safeMsg = safe(message);
        Toast.makeText(this, safeMsg.isEmpty() ? "Something went wrong." : safeMsg, Toast.LENGTH_SHORT).show();
    }

    private String safe(String value) {
        return value != null ? value.trim() : "";
    }

    private interface ErrorCallback {
        void onError(String message);
    }
}