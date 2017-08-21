package com.tiza.process.common.protocol;

import com.tiza.process.common.bean.Header;

/**
 * Description: IDataProcess
 * Author: DIYILIU
 * Update: 2017-08-04 14:12
 */

public interface IDataProcess {

    Header dealHeader(byte[] bytes);

    void parse(byte[] content, Header header);

    void init();
}
