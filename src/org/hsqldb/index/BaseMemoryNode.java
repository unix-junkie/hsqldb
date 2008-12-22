/* Copyright (c) 1995-2000, The Hypersonic SQL Group.
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
 * Neither the name of the Hypersonic SQL Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE HYPERSONIC SQL GROUP,
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Hypersonic SQL Group.
 *
 *
 * For work added by the HSQL Development Group:
 *
 * Copyright (c) 2001-2009, The HSQL Development Group
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


package org.hsqldb.index;

import java.io.IOException;

import org.hsqldb.rowio.RowOutputInterface;
import org.hsqldb.persist.*;

/**
 *  Common MEMORY and TEXT table node implementation. Nodes are always in
 *  memory so an Object reference is used to access the other Nodes in the
 *  AVL tree.
 *
 *  New class derived from the Hypersonic code
 *
 * @author Thomas Mueller (Hypersonic SQL Group)
 * @version    1.7.2
 * @since Hypersonic SQL
 */
abstract class BaseMemoryNode extends Node {

    protected Node nLeft;
    protected Node nRight;
    protected Node nParent;

    public void delete() {
        iBalance = -2;
        nLeft    = nRight = nParent = null;
    }

    Node getLeft(PersistentStore store) {
        return nLeft;
    }

    Node setLeft(PersistentStore persistentStore, Node n) {

        nLeft = n;

        return this;
    }

    public int getBalance() {
        return iBalance;
    }

    boolean isLeft(Node node) {
        return nLeft == node;
    }

    boolean isRight(Node node) {
        return nRight == node;
    }

    Node getRight(PersistentStore persistentStore) {
        return nRight;
    }

    Node setRight(PersistentStore persistentStore, Node n) {

        nRight = n;

        return this;
    }

    Node getParent(PersistentStore store) {
        return nParent;
    }

    boolean isRoot() {
        return nParent == null;
    }

    Node setParent(PersistentStore persistentStore, Node n) {

        nParent = n;

        return this;
    }

    public Node setBalance(PersistentStore store, int b) {

        iBalance = b;

        return this;
    }

    boolean isFromLeft(PersistentStore store) {

        if (this.isRoot()) {
            return true;
        }

        Node parent = getParent(store);

        return equals(parent.getLeft(store));
    }

    boolean equals(Node n) {
        return n == this;
    }

    public void write(RowOutputInterface out) throws IOException {}
}
