package de.dennisguse.notrustissues.util;

import android.content.Context;
import android.content.Intent;

public class IntentUtils {

    public static Intent newIntent(Context context, Class<?> cls) {
        return new Intent(context, cls).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
