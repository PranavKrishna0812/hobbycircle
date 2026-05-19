package com.example.hobbycircle.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnLogin;
    private Button btnRegister;
    private Button btnGoogleSignIn;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private PreferenceManager preferenceManager;
    private UserRepository userRepository;
    private GoogleSignInClient googleSignInClient;

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
                    showToast("Google sign-in failed: " + safe(e.getMessage()));
                } catch (Exception e) {
                    showToast("Unexpected error: " + safe(e.getMessage()));
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
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
        btnRegister.setOnClickListener(v -> registerWithEmailPassword());
        btnLogin.setOnClickListener(v -> loginWithEmailPassword());
        btnGoogleSignIn.setOnClickListener(v -> startGoogleSignIn());
    }

    private void registerWithEmailPassword() {
        String email = safe(textOf(etEmail));
        String password = safe(textOf(etPassword));
        String confirm = safe(textOf(etConfirmPassword));

        if (!isValidEmail(email)) {
            showToast("Enter a valid email.");
            return;
        }
        if (password.length() < 6) {
            showToast("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showToast("Password and confirm password do not match.");
            return;
        }

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

                    saveUserDocument(user, "email_password", () -> {
                        setButtonsEnabled(true);
                        loadRoleAndNavigate(user);
                    }, error -> {
                        setButtonsEnabled(true);
                        showToast(error);
                    });
                });
    }

    private void loginWithEmailPassword() {
        String email = safe(textOf(etEmail));
        String password = safe(textOf(etPassword));

        if (!isValidEmail(email)) {
            showToast("Enter a valid email.");
            return;
        }
        if (password.isEmpty()) {
            showToast("Password is required.");
            return;
        }

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
        btnLogin.setEnabled(enabled);
        btnRegister.setEnabled(enabled);
        btnGoogleSignIn.setEnabled(enabled);
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