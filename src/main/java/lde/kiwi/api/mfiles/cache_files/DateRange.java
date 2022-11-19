package lde.kiwi.api.mfiles.cache_files;

import org.joda.time.DateTime;

public class DateRange {
    public final DateTime dateFrom; 
    public final DateTime dateTo ;

    public static DateRange midnight(DateTime dateFrom,DateTime dateTo){
        return new DateRange(dateFrom, dateTo, true);
    }
    public static DateRange now(DateTime dateFrom,DateTime dateTo){
        return new DateRange(dateFrom, dateTo, false);
    }
    public static DateRange now(int daysMinuis,int daysPlus){
        return new DateRange(DateTime.now().minusDays(daysMinuis),DateTime.now().plusDays(daysPlus),false);
    }
    public static DateRange midnight(int daysMinuis,int daysPlus){
        return new DateRange(DateTime.now().minusDays(daysMinuis),DateTime.now().plusDays(daysPlus),true);
    }
    
    private DateRange(DateTime dateFrom,DateTime dateTo,boolean inclusive){
        this.dateFrom=dateFrom;
        this.dateTo=dateTo;
        if(inclusive){
            dateFrom = dateFrom.withTime(0, 0, 0, 0);
            dateTo = dateTo.withTime(0, 0, 0, 0).plusDays(1);            
        }
    }
}
