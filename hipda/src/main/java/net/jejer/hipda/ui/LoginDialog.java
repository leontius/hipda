package net.jejer.hipda.ui;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import net.jejer.hipda.R;
import net.jejer.hipda.async.LoginHelper;
import net.jejer.hipda.bean.HiSettingsHelper;
import net.jejer.hipda.utils.Constants;

/**
 * dialog for login
 * Created by GreenSkinMonster on 2015-04-18.
 */
public class LoginDialog extends Dialog {

    private static boolean isShown = false;

    private Context mCtx;
    private HiProgressDialog progressDialog;
    private Handler mHandler;

    private LoginDialog(Context context) {
        super(context);
        mCtx = context;
    }

    public static LoginDialog getInstance(Context context) {
        if (!isShown) {
            isShown = true;
            return new LoginDialog(context);
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.dialog_login, null);

        final EditText etUsername = (EditText) view.findViewById(R.id.login_username);
        final EditText etPassword = (EditText) view.findViewById(R.id.login_password);
        final Spinner spSecQuestion = (Spinner) view.findViewById(R.id.login_question);
        final EditText etSecAnswer = (EditText) view.findViewById(R.id.login_answer);

        final KeyValueArrayAdapter adapter = new KeyValueArrayAdapter(mCtx, R.layout.spinner_row);
        adapter.setEntryValues(mCtx.getResources().getStringArray(R.array.pref_login_question_list_values));
        adapter.setEntries(mCtx.getResources().getStringArray(R.array.pref_login_question_list_titles));
        spSecQuestion.setAdapter(adapter);

        etUsername.setText(HiSettingsHelper.getInstance().getUsername());
        etPassword.setText(HiSettingsHelper.getInstance().getPassword());
        if (!TextUtils.isEmpty(HiSettingsHelper.getInstance().getSecQuestion())
                && TextUtils.isDigitsOnly(HiSettingsHelper.getInstance().getSecQuestion())) {
            int idx = Integer.parseInt(HiSettingsHelper.getInstance().getSecQuestion());
            if (idx > 0 && idx < adapter.getCount())
                spSecQuestion.setSelection(idx);
        }
        etSecAnswer.setText(HiSettingsHelper.getInstance().getSecAnswer());

        Button btnLogin = (Button) view.findViewById(R.id.login_btn);
        btnLogin.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {

                InputMethodManager imm = (InputMethodManager) mCtx.getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(etPassword.getWindowToken(), 0);

                HiSettingsHelper.getInstance().setUsername(etUsername.getText().toString());
                HiSettingsHelper.getInstance().setPassword(etPassword.getText().toString());
                HiSettingsHelper.getInstance().setSecQuestion(adapter.getEntryValue(spSecQuestion.getSelectedItemPosition()));
                HiSettingsHelper.getInstance().setSecAnswer(etSecAnswer.getText().toString());
                HiSettingsHelper.getInstance().setUid("");

                progressDialog = HiProgressDialog.show(mCtx, "正在登录...");
                progressDialog.setCancelable(false);

                final LoginHelper loginHelper = new LoginHelper(mCtx, null);

                new AsyncTask<Void, Void, Integer>() {

                    @Override
                    protected Integer doInBackground(Void... voids) {
                        return loginHelper.login();
                    }

                    @Override
                    protected void onPostExecute(Integer result) {
                        if (result == Constants.STATUS_SUCCESS) {
                            Toast.makeText(mCtx, "登录成功", Toast.LENGTH_SHORT).show();
                            dismiss();
                            isShown = false;
                            if (mHandler != null) {
                                Message msg = Message.obtain();
                                msg.what = ThreadListFragment.STAGE_REFRESH;
                                mHandler.sendMessage(msg);
                            }
                        } else {
                            Toast.makeText(mCtx, loginHelper.getErrorMsg(), Toast.LENGTH_SHORT).show();
                            HiSettingsHelper.getInstance().setUsername("");
                            HiSettingsHelper.getInstance().setPassword("");
                            HiSettingsHelper.getInstance().setSecQuestion("");
                            HiSettingsHelper.getInstance().setSecAnswer("");
                            if (mHandler != null) {
                                Message msg = Message.obtain();
                                msg.what = ThreadListFragment.STAGE_ERROR;
                                Bundle b = new Bundle();
                                b.putString(ThreadListFragment.STAGE_ERROR_KEY, loginHelper.getErrorMsg());
                                msg.setData(b);
                                mHandler.sendMessage(msg);
                            }
                        }
                        progressDialog.dismiss();
                    }
                }.execute();
            }
        });

        setContentView(view);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isShown = false;
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }
}
