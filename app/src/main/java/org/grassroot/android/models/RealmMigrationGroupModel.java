package org.grassroot.android.models;

import android.util.Log;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by luke on 2016/11/24.
 */

public class RealmMigrationGroupModel implements RealmMigration {

    private static final String TAG = RealmMigrationGroupModel.class.getSimpleName();

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        Log.e(TAG, String.format("migrating: old version = %d, new version = %d", oldVersion, newVersion));

        // migrate to version 1
        if (oldVersion == 0) {
            schema.get("Group")
                    .addField("openOnChat", boolean.class);
            oldVersion++;
        }

        // skip a version because of an earlier mix up in version numbering ...
        if (oldVersion == 1) {
            oldVersion++;
        }

        // migrate to version 2
        if (oldVersion == 2) {
            schema.get("Group")
                    .addField("paidFor", boolean.class);
            oldVersion++;
            Log.e(TAG, "v3 migrated");
        }

        if (oldVersion < newVersion) {
            throw new IllegalArgumentException(String.format("Migration missing from v%d to v%d", oldVersion, newVersion));
        }

    }

}
