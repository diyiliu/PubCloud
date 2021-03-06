package com.tiza.process.common.protocol.m2.cmd;

import com.tiza.process.common.bean.Header;
import com.tiza.process.common.protocol.m2.M2DataProcess;
import com.tiza.process.common.bean.M2Header;
import com.tiza.process.common.bean.Position;
import com.tiza.process.common.bean.Status;
import org.springframework.stereotype.Service;

/**
 * Description: CMD_83
 * Author: DIYILIU
 * Update: 2017-08-03 19:15
 */

@Service
public class CMD_83 extends M2DataProcess {

    public CMD_83() {
        this.cmd = 0x83;
    }

    @Override
    public void parse(byte[] content, Header header) {
        M2Header m2Header = (M2Header) header;

        Position position = renderPosition(content);
        Status status = renderStatu(position.getStatus());

        toKafka(m2Header, position, status);
    }
}
