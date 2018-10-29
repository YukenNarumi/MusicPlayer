package com.example.yuken.musicplayer;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

public class BaseDialogFragment extends DialogFragment {
    /**
     * getActivity() の例外判定を行ったのちに返す
     *
     * @return 例外が発生しなかった場合 getActivity() を返す
     */
    protected FragmentActivity getActivityNonNull() {
        if (super.getActivity() != null) {
            return super.getActivity();
        }
        else {
            throw new RuntimeException("null returned from getActivity()");
        }
    }

    /**
     * getArguments() の例外判定を行ったのちに返す
     *
     * @return 例外が発生しなかった場合 getArguments() を返す
     */
    protected Bundle getArgumentsNonNull() {
        if (super.getArguments() != null) {
            return super.getArguments();
        }
        else {
            throw new RuntimeException("null returned from getArguments()");
        }
    }
}
