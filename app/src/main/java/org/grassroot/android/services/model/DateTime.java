package org.grassroot.android.services.model;

/**
 * Created by paballo on 2016/05/04.
 */
public class DateTime {

    private String dayOfWeek;
    private Integer dayOfYear;
    private Integer monthValue;
    private Integer hour;
    private Integer minute;
    private Integer second;
    private Integer nano;
    private Integer year;
    private String month;
    private Integer dayOfMonth;
    private Chronology chronology;

    /**
     * @return The dayOfWeek
     */
    public String getDayOfWeek() {
        return dayOfWeek;
    }

    /**
     * @param dayOfWeek The dayOfWeek
     */
    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    /**
     * @return The dayOfYear
     */
    public Integer getDayOfYear() {
        return dayOfYear;
    }

    /**
     * @param dayOfYear The dayOfYear
     */
    public void setDayOfYear(Integer dayOfYear) {
        this.dayOfYear = dayOfYear;
    }

    /**
     * @return The monthValue
     */
    public Integer getMonthValue() {
        return monthValue;
    }

    /**
     * @param monthValue The monthValue
     */
    public void setMonthValue(Integer monthValue) {
        this.monthValue = monthValue;
    }

    /**
     * @return The hour
     */
    public Integer getHour() {
        return hour;
    }

    /**
     * @param hour The hour
     */
    public void setHour(Integer hour) {
        this.hour = hour;
    }

    /**
     * @return The minute
     */
    public Integer getMinute() {
        return minute;
    }

    /**
     * @param minute The minute
     */
    public void setMinute(Integer minute) {
        this.minute = minute;
    }

    /**
     * @return The second
     */
    public Integer getSecond() {
        return second;
    }

    /**
     * @param second The second
     */
    public void setSecond(Integer second) {
        this.second = second;
    }

    /**
     * @return The nano
     */
    public Integer getNano() {
        return nano;
    }

    /**
     * @param nano The nano
     */
    public void setNano(Integer nano) {
        this.nano = nano;
    }

    /**
     * @return The year
     */
    public Integer getYear() {
        return year;
    }

    /**
     * @param year The year
     */
    public void setYear(Integer year) {
        this.year = year;
    }

    /**
     * @return The month
     */
    public String getMonth() {
        return month;
    }

    /**
     * @param month The month
     */
    public void setMonth(String month) {
        this.month = month;
    }

    /**
     * @return The dayOfMonth
     */
    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    /**
     * @param dayOfMonth The dayOfMonth
     */
    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    /**
     * @return The chronology
     */
    public Chronology getChronology() {
        return chronology;
    }

    /**
     * @param chronology The chronology
     */
    public void setChronology(Chronology chronology) {
        this.chronology = chronology;
    }

    @Override
    public String toString() {
        return "DateTime{" +
                "dayOfWeek='" + dayOfWeek + '\'' +
                ", dayOfYear=" + dayOfYear +
                ", monthValue=" + monthValue +
                ", hour=" + hour +
                ", minute=" + minute +
                ", second=" + second +
                ", nano=" + nano +
                ", year=" + year +
                ", month='" + month + '\'' +
                ", dayOfMonth=" + dayOfMonth +
                ", chronology=" + chronology +
                '}';
    }
}