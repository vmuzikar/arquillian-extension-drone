/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.arquillian.drone.appium.extension.webdriver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.remote.MobilePlatform;
import io.appium.java_client.windows.WindowsDriver;
import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.drone.spi.Configurator;
import org.jboss.arquillian.drone.spi.Destructor;
import org.jboss.arquillian.drone.spi.DronePoint;
import org.jboss.arquillian.drone.spi.Instantiator;
import org.jboss.arquillian.drone.webdriver.configuration.WebDriverConfiguration;
import org.jboss.arquillian.drone.webdriver.spi.BrowserCapabilities;
import org.jboss.arquillian.drone.webdriver.spi.BrowserCapabilitiesRegistry;
import org.openqa.selenium.Capabilities;

import java.net.URL;

import static org.arquillian.drone.appium.extension.webdriver.AppiumCapabilities.READABLE_NAME;
import static org.arquillian.drone.appium.extension.webdriver.AppiumCapabilities.REMOTE_ADDRESS;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class AppiumDriverFactory implements
    Configurator<AppiumDriver, WebDriverConfiguration>,
    Instantiator<AppiumDriver, WebDriverConfiguration>,
    Destructor<AppiumDriver> {

    @Inject
    private Instance<BrowserCapabilitiesRegistry> registryInstance;

    @Override
    public void destroyInstance(AppiumDriver instance) {
        instance.quit();
    }

    @Override
    public int getPrecedence() {
        return 0;
    }

    @Override
    public AppiumDriver createInstance(WebDriverConfiguration configuration) {
        Capabilities capabilities = configuration.getCapabilities();

        String platform = ((String)capabilities.getCapability(MobileCapabilityType.PLATFORM_NAME)).toLowerCase();
        String remoteAddr = (String)capabilities.getCapability(REMOTE_ADDRESS);

        if (StringUtils.isBlank(platform)) {
            throw new IllegalArgumentException("You have to specify " + MobileCapabilityType.PLATFORM_NAME);
        }

        Class<? extends AppiumDriver> driverClazz;

             if (MobilePlatform.ANDROID.toLowerCase().equals(platform)) driverClazz = AndroidDriver.class;
        else if (MobilePlatform.IOS.toLowerCase().equals(platform))     driverClazz = IOSDriver.class;
        else if (MobilePlatform.WINDOWS.toLowerCase().equals(platform)) driverClazz = WindowsDriver.class;
        else                                                            driverClazz = AppiumDriver.class;

        AppiumDriver driver;

        try {
            if (StringUtils.isBlank(remoteAddr)) {
                driver = driverClazz.getConstructor(Capabilities.class).newInstance(capabilities);
            }
            else {
                driver = driverClazz.getConstructor(URL.class, Capabilities.class).newInstance(remoteAddr, capabilities);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

        return driver;
    }

    @Override
    public WebDriverConfiguration createConfiguration(ArquillianDescriptor descriptor, DronePoint<AppiumDriver> dronePoint) {
        BrowserCapabilities browser = registryInstance.get().getEntryFor(READABLE_NAME);
        return new WebDriverConfiguration(browser).configure(descriptor, dronePoint.getQualifier());
    }
}
