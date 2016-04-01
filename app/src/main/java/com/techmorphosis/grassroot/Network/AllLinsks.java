package com.techmorphosis.grassroot.Network;

public class AllLinsks
{

   // public static final String DOMAIN = "http://staging.grassroot.org.za/api/";
    public static final String DOMAIN = "https://app.grassroot.org.za/api/";
    static String PREFIX1 = "user/";
    static String PREFIX2 = "group/";

    public static final String register = DOMAIN + PREFIX1 + "add/";
    public static final String verify = DOMAIN + PREFIX1 + "verify/";
    public static final String authenticate = DOMAIN + PREFIX1 + "login/authenticate/";
    public static final String login = DOMAIN + PREFIX1 + "login/";
    public static final String groupsearch = DOMAIN + PREFIX2 + "search?searchTerm=";
    public static final String joinrequest = DOMAIN + PREFIX2 + "join/request/";


}
