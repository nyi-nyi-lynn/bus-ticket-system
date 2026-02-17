package com.busticket.util;

import java.util.function.Consumer;

public final class PassengerViewRouter {
    private static Consumer<String> pageOpener;

    private PassengerViewRouter() {
    }

    public static void setPageOpener(Consumer<String> opener) {
        pageOpener = opener;
    }

    public static void open(String pageKey) {
        if (pageOpener != null) {
            pageOpener.accept(pageKey);
        }
    }
}
