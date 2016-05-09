package com.techmorphosis.grassroot.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.techmorphosis.grassroot.services.model.Member;
import com.techmorphosis.grassroot.utils.UtilClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by admin on 04-Apr-16.
 */
public class Contact implements Parcelable {

    public String addedBy;
    public boolean isSelected;
    public String name;
    public List<String> numbers;
    public String selectedNumber;
    public String contact_ID;


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
        dest.writeString(this.contact_ID);
    }

    public Contact() {
    }

    protected Contact(Parcel in) {
        this.addedBy = in.readString();
        this.isSelected = in.readByte() != 0;
        this.name = in.readString();
        this.numbers = in.createStringArrayList();
        this.selectedNumber = in.readString();
        this.contact_ID = in.readString();
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

    public static List<Member> convertToMembers(List<Contact> contacts, String roleName) {
        List<Member> members = new ArrayList<>();
        for (final Contact c : contacts)
            members.add(new Member(c.selectedNumber, c.name, roleName));
        return members;
    }

    // todo: optimize the hell out of this
    public static List<Contact> convertFromMembers(List<Member> members, boolean defaultSelected) {
        List<Contact> contacts = new ArrayList<>();
        for (final Member m : members) {
            Contact c = new Contact();
            c.selectedNumber = m.getPhoneNumber();
            c.numbers = Collections.singletonList(m.getPhoneNumber());
            c.name = m.getDisplayName();
            c.isSelected = defaultSelected;
            contacts.add(c);
        }
        return contacts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Contact contact = (Contact) o;

        return UtilClass.formatNumberToE164(selectedNumber).equals(UtilClass.formatNumberToE164(contact.selectedNumber));
    }

    @Override
    public int hashCode() {
        return selectedNumber.hashCode();
    }

    @Override
    public String toString() {
        return "Contact{" +
                "name='" + name + '\'' +
                ", selectedNumber=" + selectedNumber +
                ", numbers=" + numbers +
                '}';
    }
}