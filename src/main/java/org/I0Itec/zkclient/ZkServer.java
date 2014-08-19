/**
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.I0Itec.zkclient;

import org.I0Itec.zkclient.exception.ZkException;
import org.I0Itec.zkclient.exception.ZkInterruptedException;
import org.apache.log4j.Logger;
import org.apache.zookeeper.server.NIOServerCnxn;
import org.apache.zookeeper.server.NIOServerCnxn.Factory;
import org.apache.zookeeper.server.ZooKeeperServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class ZkServer {

    private final static Logger LOG = Logger.getLogger(ZkServer.class);

    public static final String DEFAULT_HOSTNAME= "localhost";
    public static final int DEFAULT_PORT = 2181;
    public static final int DEFAULT_TICK_TIME = 5000;
    public static final int DEFAULT_MIN_SESSION_TIMEOUT = 2 * DEFAULT_TICK_TIME;

    private String _dataDir;
    private String _logDir;

    private IDefaultNameSpace _defaultNameSpace;

    private ZooKeeperServer _zk;
    private Factory _nioFactory;
    private ZkClient _zkClient;
    private int _port;
    private int _tickTime;
    private int _minSessionTimeout;
    private String _hostname;

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace) {
        this(dataDir, logDir, defaultNameSpace, DEFAULT_PORT);
    }

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace, int port) {
        this(dataDir, logDir, defaultNameSpace, port, DEFAULT_TICK_TIME);
    }

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace, int port, int tickTime) {
        this(dataDir, logDir, defaultNameSpace, port, tickTime, DEFAULT_MIN_SESSION_TIMEOUT);
    }

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace, int port, int tickTime, int minSessionTimeout) {
        this(dataDir, logDir, defaultNameSpace, port, tickTime, minSessionTimeout, DEFAULT_HOSTNAME);
    }

    public ZkServer(String dataDir, String logDir, IDefaultNameSpace defaultNameSpace, int port, int tickTime, int minSessionTimeout, String hostname) {
        _dataDir = dataDir;
        _logDir = logDir;
        _defaultNameSpace = defaultNameSpace;
        _port = port;
        _tickTime = tickTime;
        _minSessionTimeout = minSessionTimeout;
        _hostname = hostname;
    }

    public int getPort() {
        return _port;
    }

    @PostConstruct
    public void start() {
        LOG.info(String.format("Starting ZkServer on: hostname = {}, port = {}", _hostname, _port));
        startZooKeeperServer();
        _zkClient = new ZkClient(_hostname + ":" + _port, 10000);
        _defaultNameSpace.createDefaultNameSpace(_zkClient);
    }

    private void startZooKeeperServer() {
        // check if this machine is already something running..
        if (NetworkUtil.isPortFree(_port)) {
            final File dataDir = new File(_dataDir);
            final File dataLogDir = new File(_logDir);
            dataDir.mkdirs();
            dataLogDir.mkdirs();
            // single zk server
            LOG.info("Start single zookeeper server...");
            LOG.info("data dir: " + dataDir.getAbsolutePath());
            LOG.info("data log dir: " + dataLogDir.getAbsolutePath());
            startSingleZkServer(_tickTime, dataDir, dataLogDir, _port);
        } else {
            throw new IllegalStateException("Zookeeper port " + _port + " was already in use. Running in single machine mode?");
        }
    }

    private void startSingleZkServer(final int tickTime, final File dataDir, final File dataLogDir, final int port) {
        try {
            _zk = new ZooKeeperServer(dataDir, dataLogDir, tickTime);
            _zk.setMinSessionTimeout(_minSessionTimeout);
            _nioFactory = new NIOServerCnxn.Factory(new InetSocketAddress(InetAddress.getByName(_hostname), port));
            _nioFactory.startup(_zk);
        } catch (IOException e) {
            throw new ZkException("Unable to start single ZooKeeper server.", e);
        } catch (InterruptedException e) {
            throw new ZkInterruptedException(e);
        }
    }

    @PreDestroy
    public void shutdown() {
        LOG.info("Shutting down ZkServer...");
        try {
            _zkClient.close();
        } catch (ZkException e) {
            LOG.warn("Error on closing zkclient: " + e.getClass().getName());
        }
        if (_nioFactory != null) {
            _nioFactory.shutdown();
            try {
                _nioFactory.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            _nioFactory = null;
        }
        if (_zk != null) {
            _zk.shutdown();
            _zk = null;
        }
        LOG.info("Shutting down ZkServer...done");
    }

    public ZkClient getZkClient() {
        return _zkClient;
    }
}
