package org.telegram.ui.Components;

import android.app.Activity;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.Calendar;

public class SolarCalendar {
    private Calendar calendar;
    private int date;
    private int month;
    private int weekDay;
    private int year;

    public SolarCalendar() {
        this.calendar = Calendar.getInstance();
        calSolarCalendar();
    }

    public SolarCalendar(Calendar calendar) {
        this.calendar = calendar;
        calSolarCalendar();
    }

    private void calSolarCalendar() {
        int georgianYear = calendar.get(Calendar.YEAR);
        int georgianMonth = calendar.get(Calendar.MONTH) + 1;
        int georgianDate = calendar.get(Calendar.DATE);
        weekDay = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        int[] buf1 = new int[]{0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        int[] buf2 = new int[]{0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335};
        int ld;
        if (georgianYear % 4 != 0) {
            date = buf1[georgianMonth - 1] + georgianDate;
            if (date > 79) {
                date -= 79;
                if (date <= 186) {
                    switch (date % 31) {
                        case 0:
                            month = date / 31;
                            date = 31;
                            break;
                        default:
                            month = (date / 31) + 1;
                            date %= 31;
                            break;
                    }
                    year = georgianYear - 621;
                    return;
                }
                date -= 186;
                switch (date % 30) {
                    case 0:
                        month = (date / 30) + 6;
                        date = 30;
                        break;
                    default:
                        month = (date / 30) + 7;
                        date %= 30;
                        break;
                }
                year = georgianYear - 621;
                return;
            }
            if (georgianYear <= 1996 || georgianYear % 4 != 1) {
                ld = 10;
            } else {
                ld = 11;
            }
            date += ld;
            switch (date % 30) {
                case 0:
                    month = (date / 30) + 9;
                    date = 30;
                    break;
                default:
                    month = (date / 30) + 10;
                    date %= 30;
                    break;
            }
            year = georgianYear - 622;
            return;
        }
        date = buf2[georgianMonth - 1] + georgianDate;
        if (georgianYear >= 1996) {
            ld = 79;
        } else {
            ld = 80;
        }
        if (date > ld) {
            date -= ld;
            if (date <= 186) {
                switch (date % 31) {
                    case 0:
                        month = date / 31;
                        date = 31;
                        break;
                    default:
                        month = (date / 31) + 1;
                        date %= 31;
                        break;
                }
                year = georgianYear - 621;
                return;
            }
            date -= 186;
            switch (date % 30) {
                case 0:
                    month = (date / 30) + 6;
                    date = 30;
                    break;
                default:
                    month = (date / 30) + 7;
                    date %= 30;
                    break;
            }
            year = georgianYear - 621;
            return;
        }
        date += 10;
        switch (date % 30) {
            case 0:
                month = (date / 30) + 9;
                date = 30;
                break;
            default:
                month = (date / 30) + 10;
                date %= 30;
                break;
        }
        year = georgianYear - 622;
    }

    public String getWeekDay() {
        String strWeekDay = BuildConfig.FLAVOR;
        switch (weekDay) {
            case 0:
                return LocaleController.getString("Sunday", R.string.Sunday);
            case 1:
                return LocaleController.getString("Monday", R.string.Monday);
            case 2:
                return LocaleController.getString("Tuesday", R.string.Tuesday);
            case 3:
                return LocaleController.getString("Wednesday", R.string.Wednesday);
            case 4:
                return LocaleController.getString("Thursday", R.string.Thursday);
            case 5:
                return LocaleController.getString("Friday", R.string.Friday);
            case 6:
                return LocaleController.getString("Saturday", R.string.Saturday);
            default:
                return strWeekDay;
        }
    }

    public String getMonth() {
        String strMonth = BuildConfig.FLAVOR;
        switch (month) {
            case 1:
                return LocaleController.getString("Farvardin", R.string.Farvardin);
            case 2:
                return LocaleController.getString("Ordibehesht", R.string.Ordibehesht);
            case 3:
                return LocaleController.getString("Khordad", R.string.Khordad);
            case 4:
                return LocaleController.getString("Tir", R.string.Tir);
            case 5:
                return LocaleController.getString("Mordad", R.string.Mordad);
            case 6:
                return LocaleController.getString("Shahrivar", R.string.Shahrivar);
            case 7:
                return LocaleController.getString("Mehr", R.string.Mehr);
            case 8:
                return LocaleController.getString("Aban", R.string.Aban);
            case 9:
                return LocaleController.getString("Azar", R.string.Azar);
            case 10:
                return LocaleController.getString("Dey", R.string.Dey);
            case 11:
                return LocaleController.getString("Bahman", R.string.Bahman);
            case 12:
                return LocaleController.getString("Esfand", R.string.Esfand);
            default:
                return strMonth;
        }
    }

    public String getDesDate() {
        StringBuilder describedDateFormat = new StringBuilder();
        describedDateFormat.append(String.valueOf(date)).append(" ").append(getMonth()).append(" ").append(String.valueOf(year)).append(" ").append(LocaleController.getString("Saat", R.string.Saat)).append(" ").append(getTime());
        return String.valueOf(describedDateFormat);
    }

    public String getShortDesDateTime() {
        StringBuilder describedDateFormat = new StringBuilder();
        describedDateFormat.append(String.valueOf(date)).append(" ").append(getMonth()).append(" ").append(LocaleController.getString("Saat", R.string.Saat)).append(" ").append(getTime());
        return String.valueOf(describedDateFormat);
    }

    public String getShortDesDate() {
        StringBuilder describedDateFormat = new StringBuilder();
        describedDateFormat.append(String.valueOf(date)).append(" ").append(getMonth()).append(" ");
        return String.valueOf(describedDateFormat);
    }

    public String getNumDateTime() {
        StringBuilder numericDateFormat = new StringBuilder();
        numericDateFormat.append(String.valueOf(year)).append("/").append(String.valueOf(month)).append("/").append(String.valueOf(date)).append(" ").append(LocaleController.getString("Saat", R.string.Saat)).append(" ").append(getTime());
        return String.valueOf(numericDateFormat);
    }

    public String getNumDate() {
        StringBuilder numericDateFormat = new StringBuilder();
        numericDateFormat.append(String.valueOf(year)).append("/").append(String.valueOf(month)).append("/").append(String.valueOf(date)).append(" ");
        return String.valueOf(numericDateFormat);
    }

    public String getTime() {
        boolean is24HourFormat = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Activity.MODE_PRIVATE).getBoolean("enable24HourFormat", false);
        int h = calendar.get(Calendar.HOUR_OF_DAY);
        int m = calendar.get(Calendar.MINUTE);
        StringBuilder time = new StringBuilder();
        if(!is24HourFormat) {
            int i = h < 12 ? h : h == 12 ? 12 : h - 12;
            time.append(i).append(":").append(m < 10 ? "0" + m : Integer.valueOf(m)).append(h < 12 ? " " + LocaleController.getString("AM", R.string.AM) : " " + LocaleController.getString("PM", R.string.PM));

        }
        else{
            time.append(h).append(":").append(m);
        }
        return String.valueOf(time);
    }

    public long getTimeInMillis() {
        return calendar.getTimeInMillis();
    }

    public String toString() {
        return getDesDate();
    }

    public static void main(String[] args) {
    }
}
