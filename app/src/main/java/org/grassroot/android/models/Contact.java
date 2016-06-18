package org.grassroot.android.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.grassroot.android.utils.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by admin on 04-Apr-16.
 */
public class Contact implements Parcelable, Comparable {

    public String addedBy;
    public boolean isSelected;

    public String name;
    public String firstName;
    public String lastName;

    public List<String> numbers;
    public List<String> msisdns;
    public String selectedNumber;
    public String selectedMsisdn;

    public int id;

    public Contact() {
        numbers = new ArrayList<>();
        msisdns = new ArrayList<>();
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedNumber() {
        return selectedMsisdn == null ? null : Utilities.formatNumberToE164(selectedNumber);
    }

    public String getDisplayName() {
        if (!TextUtils.isEmpty(name)) {
            return name;
        } else if (!TextUtils.isEmpty(firstName) || !TextUtils.isEmpty(lastName)) {
            return ((firstName != null) ? firstName + " " : "") + ((lastName != null) ? lastName : "");
        } else {
            return "";
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.addedBy);
        dest.writeByte(isSelected ? (byte) 1 : (byte) 0);
        dest.writeString(this.name);
        dest.writeStringList(this.numbers);
        dest.writeString(this.selectedNumber);
        dest.writeInt(this.id);
    }

    protected Contact(Parcel in) {
        this.addedBy = in.readString();
        this.isSelected = in.readByte() != 0;
        this.name = in.readString();
        this.numbers = in.createStringArrayList();
        this.selectedNumber = in.readString();
        this.id = in.readInt();
    }

    public static final Parcelable.Creator<Contact> CREATOR = new Parcelable.Creator<Contact>() {
        @Override
        public Contact createFromParcel(Parcel source) {
            return new Contact(source);
        }

        @Override
        public Contact[] newArray(int size) {
            return new Contact[size];
        }
    };

    // todo: optimize the hell out of this
    public static List<Contact> convertFromMembers(List<Member> members) {
        List<Contact> contacts = new ArrayList<>();
        for (final Member m : members) {
            Contact c = new Contact();
            c.id = -1;
            c.selectedNumber = m.getPhoneNumber();
            c.selectedMsisdn = m.getPhoneNumber();
            c.numbers = Collections.singletonList(m.getPhoneNumber());
            c.msisdns = Collections.singletonList(m.getPhoneNumber());
            c.name = m.getDisplayName();
            c.isSelected = m.isSelected();
            contacts.add(c);
        }
        return contacts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        return id == contact.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public String toString() {
        return "Contact{" +
                //"name='" + name + '\'' +
                //", firstName='" + firstName + '\'' +
                //", lastName='" + lastName + '\'' +
                // ", numbers=" + numbers +
                //", msisdns=" + msisdns +
                //", selectedNumber='" + selectedNumber + '\'' +
                ", selectedMsisdn='" + selectedMsisdn + '\'' +
                ", id='" + id + '\'' +
                '}';
    }

    @Override
    public int compareTo(Object another) {
        Contact contact = (Contact) another;
        return this.name.toLowerCase().compareTo(contact.name.toLowerCase());
    }

}