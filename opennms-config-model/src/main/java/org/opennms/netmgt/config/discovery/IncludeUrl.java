/*******************************************************************************
 * This file is part of OpenNMS(R).
 * 
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
 * 
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * 
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * 
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *     http://www.gnu.org/licenses/
 * 
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.config.discovery;


import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import org.opennms.core.xml.ValidateUsing;
import org.opennms.netmgt.config.utils.ConfigUtils;

/**
 * A file URL holding specific addresses to be polled. Each
 *  line in the URL file can be one of:
 *  "<IP><space>#<comments>", "<IP>", or
 *  "#<comments>". Lines starting with a '#' are ignored and so are
 *  characters after a '<space>#' in a line.
 */
@XmlRootElement(name = "include-url")
@XmlAccessorType(XmlAccessType.FIELD)
@ValidateUsing("discovery-configuration.xsd")
public class IncludeUrl implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * inner value
     */
    @XmlValue
    private String m_url;

    /**
     * The monitoring location where this include URL
     *  will be executed.
     */
    @XmlAttribute(name = "location")
    private String m_location;

    /**
     * The number of times a ping is retried for
     *  addresses listed in this file. If there is no response after the
     *  first ping to an address, it is tried again for the specified
     *  number of retries. This retry count overrides the
     *  default.
     */
    @XmlAttribute(name = "retries")
    private Integer m_retries;

    /**
     * The timeout on each poll for addresses listed in
     *  this file. This timeout overrides the default.
     */
    @XmlAttribute(name = "timeout")
    private Long m_timeout;

    @XmlAttribute(name = "foreign-source")
    private String m_foreignSource;

    public IncludeUrl() {
    }

    public IncludeUrl(final String url) {
        m_url = url;
    }

    public Optional<String> getUrl() {
        return Optional.ofNullable(m_url);
    }

    public void setUrl(final String url) {
        m_url = ConfigUtils.assertNotEmpty(url, "URL");
    }

    public Optional<String> getLocation() {
        return Optional.ofNullable(m_location);
    }

    public void setLocation(final String location) {
        m_location = ConfigUtils.normalizeString(location);
    }

    public Optional<Integer> getRetries() {
        return Optional.ofNullable(m_retries);
    }

    public void setRetries(final Integer retries) {
        m_retries = retries;
    }

    public Optional<Long> getTimeout() {
        return Optional.ofNullable(m_timeout);
    }

    public void setTimeout(final Long timeout) {
        if (timeout != null && timeout == 0) {
            throw new IllegalArgumentException("Can't have a 0 timeout!");
        }
        m_timeout = timeout;
    }

    public Optional<String> getForeignSource() {
        return Optional.ofNullable(m_foreignSource);
    }

    public void setForeignSource(final String foreignSource) {
        m_foreignSource = ConfigUtils.normalizeString(foreignSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                            m_url, 
                            m_location, 
                            m_retries, 
                            m_timeout, 
                            m_foreignSource);
    }

    @Override
    public boolean equals(final Object obj) {
        if ( this == obj ) {
            return true;
        }

        if (obj instanceof IncludeUrl) {
            final IncludeUrl temp = (IncludeUrl)obj;
            return Objects.equals(temp.m_url, m_url)
                    && Objects.equals(temp.m_location, m_location)
                    && Objects.equals(temp.m_retries, m_retries)
                    && Objects.equals(temp.m_timeout, m_timeout)
                    && Objects.equals(temp.m_foreignSource, m_foreignSource);
        }
        return false;
    }

    @Override
    public String toString() {
        return "IncludeUrl [value=" + m_url + ", location="
                + m_location + ", retries=" + m_retries + ", timeout="
                + m_timeout + ", foreignSource=" + m_foreignSource + "]";
    }

}
