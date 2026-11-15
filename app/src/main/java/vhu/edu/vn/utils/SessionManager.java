package vhu.edu.vn.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private final SharedPreferences pref;

    public SessionManager(Context context) {
        pref = context.getApplicationContext().getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void createLoginSession(int userId, String username, String fullName, String role) {
        pref.edit()
                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                .putInt(Constants.KEY_USER_ID, userId)
                .putString(Constants.KEY_USERNAME, username)
                .putString(Constants.KEY_FULL_NAME, fullName)
                .putString(Constants.KEY_ROLE, role)
                .apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(Constants.KEY_IS_LOGGED_IN, false) && getUserId() > 0;
    }

    public int getUserId() {
        return pref.getInt(Constants.KEY_USER_ID, -1);
    }

    public String getUsername() {
        return pref.getString(Constants.KEY_USERNAME, "");
    }

    public String getFullName() {
        return pref.getString(Constants.KEY_FULL_NAME, "");
    }

    public String getRole() {
        return pref.getString(Constants.KEY_ROLE, "");
    }

    public boolean isStaff() {
        String role = getRole();
        return Constants.ROLE_MANAGER.equals(role)
                || Constants.ROLE_RECEPTIONIST.equals(role)
                || Constants.ROLE_TECHNICIAN.equals(role);
    }

    public void logoutUser() {
        pref.edit().clear().apply();
    }
}
