package org.grassroot.android.models;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

/**
 * Created by luke on 2016/11/24.
 */

public class RealmMigrationGroupChat implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();

        if (oldVersion == 0) {
            schema.get("Group")
                    .addField("openOnChat", boolean.class);
            oldVersion++;
        }

    }

}
