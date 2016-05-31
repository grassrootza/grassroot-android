package org.grassroot.android.services.model;

import android.util.Log;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by luke on 2016/05/18.
 */
public class MemberDeserializer implements JsonDeserializer<Member> {

    private static final String TAG = MemberDeserializer.class.getCanonicalName();

    @Override
    public Member deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {

        Log.e(TAG, "deserializing, in custom converter");

        final JsonObject jsonObject = json.getAsJsonObject();

        final String phoneNumber = jsonObject.get("phoneNumber").getAsString();
        final String userUid = jsonObject.get("memberUid").getAsString();
        final String groupUid = jsonObject.get("groupUid").getAsString();
        final String displayName = jsonObject.get("displayName").getAsString();
        final String roleName = jsonObject.get("roleName").getAsString();

        final Member member = new Member(phoneNumber, displayName, roleName, null);
        member.setMemberUid(userUid);
        member.setSelected(true);

        return member;
    }

}
