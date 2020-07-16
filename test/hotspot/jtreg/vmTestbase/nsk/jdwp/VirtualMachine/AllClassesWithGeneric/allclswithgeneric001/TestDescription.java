/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


/*
 * @test
 *
 * @summary converted from VM Testbase nsk/jdwp/VirtualMachine/AllClassesWithGeneric/allclswithgeneric001.
 * VM Testbase keywords: [jpda, jdwp]
 * VM Testbase readme:
 * DESCRIPTION
 *     This test performs checking for
 *         JDWP command set: VirtualMachine
 *         JDWP command: AllClassesWithGeneric
 *     It checks that the command returns generic signature information
 *     properly.
 *     Debuggee part of the test creates instances of several tested classes.
 *     Some of the classes are generic. Debugger part obtains signature
 *     information of all loaded classes by sending the tested JDWP command.
 *     Proper generic signature should be returned for each tested class
 *     which is generic, or an empty string for non-generic one. All tested
 *     classes should be found in reply packet as well.
 * COMMENTS
 *
 * @library /vmTestbase /test/hotspot/jtreg/vmTestbase
 *          /test/lib
 * @build nsk.jdwp.VirtualMachine.AllClassesWithGeneric.allclswithgeneric001
 *        nsk.jdwp.VirtualMachine.AllClassesWithGeneric.allclswithgeneric001t
 * @run main/othervm PropertyResolvingWrapper
 *      nsk.jdwp.VirtualMachine.AllClassesWithGeneric.allclswithgeneric001
 *      -arch=${os.family}-${os.simpleArch}
 *      -verbose
 *      -waittime=5
 *      -debugee.vmkind=java
 *      -transport.address=dynamic
 *      -debugee.vmkeys="${test.vm.opts} ${test.java.opts}"
 */

