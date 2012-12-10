package it0003;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

public class HelloWorldJNI
{
    static
    {
        NarSystem.loadLibrary();
    }

    public final native String sayHello();
    public final native int timesHello(int x, int y);

    public static void main( String[] args )
    {
        HelloWorldJNI app = new HelloWorldJNI();
        System.out.println( app.sayHello() );

	System.out.println( app.timesHello(5, 7) );
    }
}
