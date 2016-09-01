package org.grassroot.android.models.responses;

import com.google.gson.annotations.SerializedName;

import org.grassroot.android.models.PublicGroupModel;

import java.util.List;

import io.realm.RealmList;

/**
 * Created by paballo on 2016/05/05.
 */
public class GroupSearchResponse extends AbstractResponse {

	@SerializedName("data")
	private RealmList<PublicGroupModel> data = new RealmList<>();

	public List<PublicGroupModel> getGroups() {
                return data;
                }

	public void setData(RealmList<PublicGroupModel> data) { this.data = data; }

}
