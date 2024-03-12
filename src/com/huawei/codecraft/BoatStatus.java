package com.huawei.codecraft;

public enum BoatStatus {
    IN_ORIGIN_POINT,//在虚拟点，切换泊位是虚拟点到泊位时间
    IN_BERTH_WAIT,//在泊位外等待,切换泊位是泊位切换时间,回家是虚拟点到泊位时间
    IN_BERTH_INTER,//在泊位,切换泊位是泊位切换时间,回家是虚拟点到泊位时间
    IN_ORIGIN_TO_BERTH,//在虚拟点到泊位移动中，切换泊位是虚拟点到泊位时间
    IN_BERTH_TO_BERTH,//在泊位到泊位移动中,泊位切换时间,回家是虚拟点到泊位时间
}
