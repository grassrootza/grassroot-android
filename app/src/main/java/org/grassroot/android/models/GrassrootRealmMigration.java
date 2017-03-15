package org.grassroot.android.models;

import android.util.Log;

import io.realm.DynamicRealm;
import io.realm.FieldAttribute;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by luke on 2016/11/24.
 */

public class GrassrootRealmMigration implements RealmMigration {

    private static final String TAG = GrassrootRealmMigration.class.getSimpleName();

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

        if (oldVersion == 3) {
            schema.get("Message")
                    .addField("taskType", String.class);
            oldVersion++;
            Log.e(TAG, "v4 migrated");
        }

        // another skip because of mess up in realm counting before [!]

        if (oldVersion == 4) {
            Log.e(TAG, "about to create ImageRecord, if it doesn't exist");
            if (!schema.contains("ImageRecord")) {
                schema.create("ImageRecord")
                        .addField("key", String.class, FieldAttribute.PRIMARY_KEY)
                        .addField("actionLogType", String.class)
                        .addField("taskUid", String.class)
                        .addField("bucket", String.class)
                        .addField("creationTime", Long.class)
                        .addField("storageTime", Long.class)
                        .addField("md5", String.class);
            }
            Log.e(TAG, "migrated to v5");
            oldVersion++;
        }

        if (oldVersion == 5) {
            Log.e(TAG, "about add fields to ImageRecord");
            schema.get("ImageRecord")
                    .addField("userDisplayName", String.class)
                    .addField("latitude", Double.class)
                    .addField("longitude", Double.class);
            Log.e(TAG, "migrated to v6");
            oldVersion++;
        }

        if (oldVersion < newVersion) {
            throw new IllegalArgumentException(String.format("Migration missing from v%d to v%d", oldVersion, newVersion));
        }

    }

}
