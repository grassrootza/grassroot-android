package com.techmorphosis.grassroot.Network;

public class AllLinsks
{

    public static final String DOMAIN2 = "http://staging.grassroot.org.za/api/";
   // public static final String DOMAIN = "https://app.grassroot.org.za/api/";
    static String PREFIX1 = "user/";
    static String PREFIX2 = "group/";
    static String PREFIX3 = "task/";

    public static final String register = DOMAIN2 + PREFIX1 + "add/";
    public static final String verify = DOMAIN2 + PREFIX1 + "verify/";
    public static final String authenticate = DOMAIN2 + PREFIX1 + "login/authenticate/";
    public static final String login = DOMAIN2 + PREFIX1 + "login/";
    public static final String groupsearch = DOMAIN2 + PREFIX2 + "search?searchTerm=";
    public static final String joinrequest = DOMAIN2 + PREFIX2 + "join/request/";
    public static final String groupcreation = DOMAIN2 + PREFIX2 + "create/";
    public static final String usergroups = DOMAIN2 + PREFIX2 + "list/";
    public static final String groupactivities = DOMAIN2 + PREFIX3 + "list/";
    public static final String Vote= DOMAIN2 + "vote/do/";
    public static final String Meeting = DOMAIN2 + "meeting/rsvp/";
    public static final String ToDo = DOMAIN2 + "logbook/complete/do/";


}
