package com.example.study_buddy;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Controller for the Login and Sign-Up portal.
 * Manages user authentication, account creation, input validation, and view switching.
 */
public class LoginController {

    // Card containers
    @FXML private VBox loginCard;
    @FXML private VBox signupCard;

    // Login components
    @FXML private TextField loginIdentifierField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Label loginMessageLabel;
    @FXML private Button loginButton;

    // Sign-Up components
    @FXML private TextField signupEmailField;
    @FXML private TextField signupUsernameField;
    @FXML private PasswordField signupPasswordField;
    @FXML private PasswordField signupConfirmPasswordField;
    @FXML private Label signupMessageLabel;
    @FXML private Button signupButton;

    /**
     * Switches to the Sign-Up view.
     */
    @FXML
    public void showSignUpCard() {
        loginCard.setVisible(false);
        loginCard.setManaged(false);
        signupCard.setVisible(true);
        signupCard.setManaged(true);
        clearMessages();
    }

    /**
     * Switches to the Login view.
     */
    @FXML
    public void showLoginCard() {
        signupCard.setVisible(false);
        signupCard.setManaged(false);
        loginCard.setVisible(true);
        loginCard.setManaged(true);
        clearMessages();
    }

    /**
     * Handles user login with either Username or Gmail.
     */
    @FXML
    public void handleLogin() {
        clearMessages();

        String identifier = loginIdentifierField.getText() != null ? loginIdentifierField.getText().trim() : "";
        String password = loginPasswordField.getText() != null ? loginPasswordField.getText() : "";

        if (identifier.isEmpty() || password.isEmpty()) {
            setErrorMessage(loginMessageLabel, "Please enter both username/email and password.");
            return;
        }

        User user = DatabaseHelper.authenticateUser(identifier, password);
        if (user != null) {
            // Login successful: navigate to main dashboard
            navigateToDashboard(user);
        } else {
            setErrorMessage(loginMessageLabel, "Invalid username/email or password. Please try again.");
        }
    }

    /**
     * Handles user sign-up with Gmail, username, and password.
     */
    @FXML
    public void handleSignUp() {
        clearMessages();

        String email = signupEmailField.getText() != null ? signupEmailField.getText().trim() : "";
        String username = signupUsernameField.getText() != null ? signupUsernameField.getText().trim() : "";
        String password = signupPasswordField.getText() != null ? signupPasswordField.getText() : "";
        String confirmPassword = signupConfirmPasswordField.getText() != null ? signupConfirmPasswordField.getText() : "";

        // 1. Validate Email (must be a valid Gmail address)
        if (email.isEmpty()) {
            setErrorMessage(signupMessageLabel, "Please enter your Gmail address.");
            return;
        }
        if (!isValidGmail(email)) {
            setErrorMessage(signupMessageLabel, "Please enter a valid Gmail address (ending with @gmail.com).");
            return;
        }

        // 2. Validate Username
        if (username.isEmpty()) {
            setErrorMessage(signupMessageLabel, "Please enter a username.");
            return;
        }
        if (username.length() < 3) {
            setErrorMessage(signupMessageLabel, "Username must be at least 3 characters.");
            return;
        }

        // 3. Validate Strong Password
        String passwordValidationError = validateStrongPassword(password);
        if (passwordValidationError != null) {
            setErrorMessage(signupMessageLabel, passwordValidationError);
            return;
        }
        if (!password.equals(confirmPassword)) {
            setErrorMessage(signupMessageLabel, "Passwords do not match. Please re-type.");
            return;
        }

        // 4. Save to SQLite database
        String error = DatabaseHelper.registerUser(email, username, password);
        if (error != null) {
            setErrorMessage(signupMessageLabel, error);
        } else {
            // Successful registration: switch to login view and prefill username
            showLoginCard();
            loginIdentifierField.setText(username);
            loginPasswordField.clear();
            setSuccessMessage(loginMessageLabel, "Account created successfully! You can now log in.");
        }
    }

    /**
     * Validates that an email ends with @gmail.com and has a name before the @.
     */
    private boolean isValidGmail(String email) {
        String lower = email.toLowerCase();
        return lower.endsWith("@gmail.com") && lower.length() > "@gmail.com".length();
    }

    /**
     * Validates that a password satisfies strong password criteria:
     * - Minimum 8 characters
     * - At least one uppercase letter (A-Z)
     * - At least one lowercase letter (a-z)
     * - At least one digit/number (0-9)
     * - At least one special character/symbol
     * Returns null if valid, or a descriptive error message explaining the missing requirement.
     */
    public static String validateStrongPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Please set a password.";
        }
        if (password.length() < 8) {
            return "Password must be at least 8 characters long.";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "Password must contain at least one capital letter (A-Z).";
        }
        if (!password.matches(".*[a-z].*")) {
            return "Password must contain at least one lowercase letter (a-z).";
        }
        if (!password.matches(".*[0-9].*")) {
            return "Password must contain at least one number/digit (0-9).";
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?~`].*")) {
            return "Password must contain at least one special character (e.g. @, #, $, !).";
        }
        return null; // Strong password valid!
    }

    /**
     * Opens the main dashboard scene once authenticated.
     */
    private void navigateToDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("hello-view.fxml"));
            Parent root = loader.load();

            // Pass user details to the dashboard controller
            HelloController controller = loader.getController();
            controller.initUser(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Study Buddy - Class Routine Dashboard");
            stage.setResizable(true);
            stage.setMaximized(true);
        } catch (IOException e) {
            setErrorMessage(loginMessageLabel, "Error loading main screen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void clearMessages() {
        loginMessageLabel.setText("");
        loginMessageLabel.getStyleClass().removeAll("label-error", "label-success");
        signupMessageLabel.setText("");
        signupMessageLabel.getStyleClass().removeAll("label-error", "label-success");
    }

    private void setErrorMessage(Label label, String message) {
        label.setText(message);
        label.getStyleClass().removeAll("label-error", "label-success");
        label.getStyleClass().add("label-error");
    }

    private void setSuccessMessage(Label label, String message) {
        label.setText(message);
        label.getStyleClass().removeAll("label-error", "label-success");
        label.getStyleClass().add("label-success");
    }
}
