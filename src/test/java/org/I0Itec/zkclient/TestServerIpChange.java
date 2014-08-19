package org.I0Itec.zkclient;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

/**
 * To test this unit testing, the followings should be done first
 *
 * ====== pre-test setup ======
 * # bind 192.168.1.1 and 192.168.1.2 to lo0 interface as alias
 * sudo ifconfig lo0 192.168.1.1 alias
 * sudo ifconfig lo0 192.168.1.2 alias
 *
 * ====== manual test #1 ======
 * - add mapping to /etc/hosts
 *    192.168.1.1 zkserver
 * - run test. it should stuck in the line of second client.createEphemeral(...)
 * - change mapping in /etc/hosts
 *    192.168.1.2 zkserver
 * - test should proceed and finish
 *
 * ====== manual test #2 ======
 * - set breakpoint in the line of second TestUtil.startZkServer(...)
 * - add mapping to /etc/hosts
 *   192.168.1.1 zkserver
 * - debug test. it should stuck in breakpoint
 * - remove mapping in /etc/hosts
 *   192.168.1.1 zkserver
 * - wait for 2 mintues. you should see a few retries of resetConnection with UnknownHostException
 * - add mapping in /etc/hosts
 *    192.168.1.2 zkserver
 * - test should proceed and finish
 *
 */
@Ignore
public class TestServerIpChange {

    @Test
    public void test() throws InterruptedException, IOException {
        ZkServer _zkServer = TestUtil.startZkServer("Zk_SERVER_IP_Change", "192.168.1.1", 2181);

        ZkClient client = new ZkClient("zkserver:2181", 5000);
        client.createEphemeral("/a");
        for (int i = 0; i < 10; ++i) {
            System.out.println("send and receive data: " + i);
            client.readData("/a");
            client.writeData("/a", Integer.toString(i));
            Thread.sleep(1000);
            System.out.println("sleeping..." + i);
        }
        _zkServer.shutdown();

        _zkServer = TestUtil.startZkServer("Zk_SERVER_IP_Change", "192.168.1.2", 2181);
        // change /etc/hosts
        client.createEphemeral("/a");
        for (int i = 0; i < 10; ++i) {
            System.out.println("send and receive data: " + i);
            client.readData("/a");
            client.writeData("/a", Integer.toString(i));
            Thread.sleep(1000);
            System.out.println("sleeping..." + i);
        }
        _zkServer.shutdown();
    }

}
