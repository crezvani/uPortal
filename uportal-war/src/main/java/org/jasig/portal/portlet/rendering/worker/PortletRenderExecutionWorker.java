/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.portlet.rendering.worker;

import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.portal.portlet.om.IPortletWindowId;
import org.jasig.portal.portlet.rendering.IPortletRenderer;
import org.jasig.portal.portlet.rendering.PortletRenderResult;

/**
 * {@link PortletExecutionWorker} capable of rendering the body markup
 * for a portlet.
 * 
 * @see IPortletRenderer#doRenderMarkup(IPortletWindowId, HttpServletRequest, HttpServletResponse, java.io.Writer)
 * @author Eric Dalquist
 * @version $Revision$
 */
class PortletRenderExecutionWorker extends PortletExecutionWorker<PortletRenderResult> implements IPortletRenderExecutionWorker {
    private String output = null;
    
    public PortletRenderExecutionWorker(
            ExecutorService executorService, List<IPortletExecutionInterceptor> interceptors, IPortletRenderer portletRenderer, 
            HttpServletRequest request, HttpServletResponse response, IPortletWindowId portletWindowId, String portletFname) {
        
        super(executorService, interceptors, portletRenderer, request, response, portletWindowId, portletFname);
    }

    @Override
    public ExecutionType getExecutionType() {
        return ExecutionType.RENDER;
    }

    @Override
    protected PortletRenderResult callInternal() throws Exception {
        final StringWriter writer = new StringWriter();
        final PortletRenderResult result = portletRenderer.doRenderMarkup(portletWindowId, request, response, writer);
        this.output = writer.toString();
        return result;
    }

    @Override
    public String getOutput(long timeout) throws Exception {
        this.get(timeout);
        return this.output;
    }
}