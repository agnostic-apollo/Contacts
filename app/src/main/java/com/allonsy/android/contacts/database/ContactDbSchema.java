package com.allonsy.android.contacts.database;


public class ContactDbSchema {
    public static final class PersonTable {
        public static final String NAME = "person";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String NAME = "name";
        }
    }

    public static final class PhoneTable {
        public static final String NAME = "phones";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String PHONE = "phone";
        }
    }

    public static final class EmailTable {
        public static final String NAME = "emails";

        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String EMAIL = "email";
        }
    }
}
