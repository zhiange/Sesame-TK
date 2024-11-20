package fansirsqi.xposed.sesame.rpc.intervallimit;

public interface IntervalLimit {

    Integer getInterval();

    Long getTime();

    void setTime(Long time);

}