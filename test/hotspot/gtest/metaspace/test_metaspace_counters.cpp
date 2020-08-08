/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2018, 2020 SAP SE. All rights reserved.
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
 *
 */

#include "precompiled.hpp"

//#define LOG_PLEASE

#include "classfile/classLoaderData.hpp"
#include "metaspaceTestsCommon.hpp"
#include "metaspace_rangehelpers.hpp"
#include "threadHelper.inline.hpp"


TEST_VM(metaspace, counters_basic)   {

  IntCounter cnt;
  ASSERT_0(cnt.get());
  cnt.increment();
  ASSERT_EQ(cnt.get(), (unsigned)1);
  cnt.increment_by(100);
  ASSERT_EQ(cnt.get(), (unsigned)101);
  cnt.decrement_by(101);
  ASSERT_0(cnt.get());

}

