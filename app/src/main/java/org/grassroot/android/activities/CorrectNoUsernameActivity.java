package org.grassroot.android.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.grassroot.android.R;
import org.grassroot.android.fragments.SingleInputFragment;
import org.grassroot.android.models.PreferenceObject;
import org.grassroot.android.models.responses.GenericResponse;
import org.grassroot.android.services.GrassrootRestService;
import org.grassroot.android.utils.RealmUtils;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by luke on 2017/08/04.
 */

public class CorrectNoUsernameActivity extends PortraitActivity {

    private static final String TAG = CorrectNoUsernameActivity.class.getSimpleName();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_correct_username);

        SingleInputFragment inputFragment = new SingleInputFragment.SingleInputBuilder()
                .header(R.string.correct_user_title)
                .explanation(R.string.correct_user_explain)
                .hint(R.string.correct_user_hint)
                .next(android.R.string.ok)
                .subscriber(new Consumer<String>() {
                    @Override
                    public void accept(@NonNull String s) throws Exception {
                        sendToServer(s);
                    }
                })
                .build();

        getSupportFragmentManager().beginTransaction()
                .add(R.id.username_fragment, inputFragment)
                .commit();
    }

    // todo : validation
    private void sendToServer(final String s) {
        final String phoneNumber = RealmUtils.loadPreferencesFromDB().getMobileNumber();
        final String token = RealmUtils.loadPreferencesFromDB().getToken();
        GrassrootRestService.getInstance().getApi().renameUser(phoneNumber, token, s)
                .enqueue(new Callback<GenericResponse>() {
                    @Override
                    public void onResponse(Call<GenericResponse> call, Response<GenericResponse> response) {
                        Log.e(TAG, "renamed!");
                        handleSuccessAndCleanUp(s);
                    }

                    @Override
                    public void onFailure(Call<GenericResponse> call, Throwable t) {
                        Log.e(TAG, "failed!", t);
                        handleErrorAndContinue();
                    }
                });
    }

    private void handleSuccessAndCleanUp(final String userName) {
        PreferenceObject preferenceObject = RealmUtils.loadPreferencesFromDB();
        preferenceObject.setUserName(userName);
        RealmUtils.saveDataToRealmWithSubscriber(preferenceObject);
        Toast.makeText(this, R.string.correct_user_done, Toast.LENGTH_SHORT).show();
        startActivity(chooseNextActivity());
        finish();
    }

    private void handleErrorAndContinue() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.correct_user_error_dialog)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(chooseNextActivity());
                        finish();
                    }
                });
        builder.show();
    }

    private Intent chooseNextActivity() {
        return RealmUtils.loadPreferencesFromDB().isHasGroups() ?
                new Intent(CorrectNoUsernameActivity.this, HomeScreenActivity.class) :
                new Intent(CorrectNoUsernameActivity.this, NoGroupWelcomeActivity.class);
    }

}
