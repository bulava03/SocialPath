package com.example.SocialPath.helper;

import java.util.HashMap;
import java.util.Map;

public class ConvertHelper {

    public static String monthToString(int month) {
        Map<Integer, String> months = new HashMap<>();
        months.put(1, "Січень");
        months.put(2, "Лютий");
        months.put(3, "Березень");
        months.put(4, "Квітень");
        months.put(5, "Травень");
        months.put(6, "Червень");
        months.put(7, "Липень");
        months.put(8, "Серпень");
        months.put(9, "Вересень");
        months.put(10, "Жовтень");
        months.put(11, "Листопад");
        months.put(12, "Грудень");

        if (months.containsKey(month)) {
            return months.get(month);
        } else {
            throw new IllegalArgumentException("Невірне значення місяця.");
        }
    }

}
