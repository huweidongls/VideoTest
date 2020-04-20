package com.jingna.videotest.lechange.Dialog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.jingna.videotest.R;
import com.jingna.videotest.lechange.listener.PasswordSetListener;

public class PasswordDialog extends DialogFragment {


    private PasswordSetListener mListener;
//    private EditText mEditText;
    private EditText editText1;

    private int currentType =-1;  //1  表示设备密码 2表示wifi密码 设备初始化的密码

    @Override
    public void setStyle(int style, int theme) {
        super.setStyle(style, theme);
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    @Override
    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
    }

    @Override
    public boolean isCancelable() {
        return super.isCancelable();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * 注释原因：wifi密码从设备添加页面携带过来，不再从密码框输入中获取
     * @param savedInstanceState
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        mEditText = new EditText(getActivity());
        editText1=new EditText(getActivity());
//        mEditText.setHint(getResources().getString(R.string.please_input_connect_wifi_password));
        editText1.setHint(getResources().getString(R.string.please_input_init_password));
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage("请输入密码");
        LinearLayout linearLayout=new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
//        linearLayout.addView(mEditText);
        linearLayout.addView(editText1);
        builder.setView(linearLayout);
        if (currentType == 1) {
            editText1.setHint(getResources().getString(R.string.please_input_init_password));
            editText1.setVisibility(View.VISIBLE);
        }else if(currentType==2){
            editText1.setHint(getResources().getString(R.string.please_input_init_password_old));
            editText1.setVisibility(View.VISIBLE);
        }else{
            editText1.setVisibility(View.GONE);
        }

        builder.setPositiveButton(getResources().getString(R.string.dialog_positive), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (null != mListener) {
                    if (currentType == 1 || currentType == 2) {
//                        mListener.onSaveSuccess(mEditText.getText().toString(),editText1.getText().toString());
                        mListener.onSaveSuccess(editText1.getText().toString());
                    } else {
//                        mListener.onWifiPassWord(mEditText.getText().toString(),editText1.getText().toString());
                        mListener.onWifiPassWord(editText1.getText().toString());
                    }
                }

            }
        });
        builder.setNegativeButton(getResources().getString(R.string.dialog_nagative), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
        builder.setCancelable(false);
        AlertDialog alertDialog = builder.create();
        return alertDialog;
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }



    public void setListener(PasswordSetListener listener) {
        this.mListener = listener;

    }


    public void setCurrentType(int currentType) {
        this.currentType = currentType;
    }
}