package com.tiza.process.common.protocol.m2.cmd;

import com.tiza.process.common.bean.Header;
import com.tiza.process.common.protocol.m2.M2DataProcess;
import org.springframework.stereotype.Service;

/**
 * Description: CMD_8C
 * Author: DIYILIU
 * Update: 2017-08-03 19:15
 */

@Service
public class CMD_8C extends M2DataProcess {

    public CMD_8C() {
        this.cmd = 0x8C;
    }

    @Override
    public void parse(byte[] content, Header header) {
        super.parse(content, header);
    }
}
