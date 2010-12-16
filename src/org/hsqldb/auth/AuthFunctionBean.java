/* Copyright (c) 2001-2010, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package org.hsqldb.auth;

/**
 * N.b. AuthFunctionBeans are NOT directly usable as HyperSQL Authentication
 * Function methods, they are POJO beans to be managed by AuthBeanMultiplexer
 * (which does have a real HyperSQL Authentication Function static method).
 *
 * @see AuthBeanMultiplexer for how these beans are used.
 * @author Blaine Simpson (blaine dot simpson at admc dot com)
 * @since 2.0.1
 */
public interface AuthFunctionBean {
    /**
     * Return a list of authorized roles or null to indicate that the
     * implementation does not intend to produce a specific role list but only
     * to indicate whether to allow access or not.
     * A return value of String[0] is different from returning null, and means
     * that the user should not be granted any roles.
     *
     * @throws Exception If user should not be allowed access to the specified
     *         database.  Other registed AuthFunctionBeans will not be attempted.
     * @throws RuntimeException Upon system problem.  The exception will be
     *         logged to the HyperSQL application logger and other registered
     *         AuthFunctionBeans (if any) will be attempted.
     * @return null or String[] according to the contract of HyperSQL
     *         authentication function contract, except that the role/schema
     *         list is returned as a String[] instead of a java.sql.Array.
     */
    public String[] authenticate(
            String userName, String password) throws Exception;
}
