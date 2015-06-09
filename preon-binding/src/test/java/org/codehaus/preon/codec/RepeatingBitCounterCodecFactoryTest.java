/**
 * Copyright (C) 2009-2010 Wilfred Springer
 * Copyright (C) 2015 Garth Dahlstrom, 2Keys Corporation
 *
 * This file is part of Preon.
 *
 * Preon is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * Preon is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Preon; see the file COPYING. If not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is making a
 * combined work based on this library. Thus, the terms and conditions of the
 * GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules, and
 * to copy and distribute the resulting executable under terms of your choice,
 * provided that you also meet, for each linked independent module, the terms
 * and conditions of the license of that module. An independent module is a
 * module which is not derived from or based on this library. If you modify this
 * library, you may extend this exception to your version of the library, but
 * you are not obligated to do so. If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.codehaus.preon.codec;

import org.codehaus.preon.el.Expression;
import org.codehaus.preon.Builder;
import org.codehaus.preon.Codec;
import org.codehaus.preon.DecodingException;
import org.codehaus.preon.Resolver;
import org.codehaus.preon.annotation.BoundRepeatingBitCounter;
import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.buffer.DefaultBitBuffer;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.AnnotatedElement;
import java.nio.ByteBuffer;

import javax.xml.bind.DatatypeConverter;

/* 
 * Unit test for Repeating Bit Counter decoder
 * 
 * @author Garth Dahlstrom, 2Keys Corporation
 */
@RunWith(MockitoJUnitRunner.class)
public class RepeatingBitCounterCodecFactoryTest {

	@Mock
	private Resolver resolver;

	@Mock    
	private Builder builder;

	@Mock
	private Expression<Integer, Resolver> size;

	@Mock
	private AnnotatedElement metadata;

	@Mock
	private BoundRepeatingBitCounter settings;

	@Mock
	private RepeatingBitCounterCodecFactory factory;

	// http://stackoverflow.com/questions/9739121/convert-a-pem-formatted-string-to-a-java-security-cert-x509certificate
	public static String toHexString(byte[] array) {
		return (array == null ? "null" : DatatypeConverter.printHexBinary(array));
	}

	public static byte[] toByteArray(String s) {
		return DatatypeConverter.parseHexBinary(s.replaceAll(" ", ""));
	}

	@Before
	public void setUp() {
		metadata = EasyMock.createMock(AnnotatedElement.class);
		settings = createMock(BoundRepeatingBitCounter.class);

		factory = new RepeatingBitCounterCodecFactory();

		builder = createMock(Builder.class);
	}

	@Test
	public void terminateBit1() throws DecodingException {    	
		expect(settings.maxCount()).andReturn("").anyTimes();
		expect(settings.terminateBit()).andReturn((byte) 1).anyTimes();    	

		expect(metadata.isAnnotationPresent(BoundRepeatingBitCounter.class)).andReturn(true);
		expect(metadata.getAnnotation(BoundRepeatingBitCounter.class)).andReturn(settings);

		replay(settings, metadata, builder);

		String bits = "21 00 00 FF"; // Dec: 65536 -- Hex result: 0x10000 (xxT0 0001, 0000 0000, 0000 0000 || 1111 1111)		
		BitBuffer buffer = new DefaultBitBuffer(ByteBuffer.wrap(toByteArray(bits)));

		Codec<Integer> codec = factory.create(metadata, Integer.class, null);

		assertEquals(new Integer(3), codec.decode(buffer, resolver, builder));

		verify(metadata, settings, builder);
	}

	@Test
	public void maxCount5() throws DecodingException {    	
		expect(settings.maxCount()).andReturn("5").anyTimes();
		expect(settings.terminateBit()).andReturn((byte) 0).anyTimes();    	

		expect(metadata.isAnnotationPresent(BoundRepeatingBitCounter.class)).andReturn(true);
		expect(metadata.getAnnotation(BoundRepeatingBitCounter.class)).andReturn(settings);

		replay(settings, metadata, builder);

		String bits = "FF FF"; // Dec: 65535 -- Hex result: 0xFFFF (xxxx xxxx, xxxx xxxx)		
		BitBuffer buffer = new DefaultBitBuffer(ByteBuffer.wrap(toByteArray(bits)));

		Codec<Integer> codec = factory.create(metadata, Integer.class, null);

		assertEquals(new Integer(5), codec.decode(buffer, resolver, builder));

		verify(metadata, settings, builder);
	}

	@Test
	public void shouldDecodeCorrectly() throws DecodingException {
		expect(settings.maxCount()).andReturn("").anyTimes();
		expect(settings.terminateBit()).andReturn((byte) 0).anyTimes();    	

		expect(metadata.isAnnotationPresent(BoundRepeatingBitCounter.class)).andReturn(true);
		expect(metadata.getAnnotation(BoundRepeatingBitCounter.class)).andReturn(settings);

		replay(settings, metadata, builder);

		Codec<Integer> codec = factory.create(metadata, Integer.class, null);

		assertEquals(new Integer(1), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("04 01 FF"))), resolver, builder));
				// Dec: 4 -- Hex result: 04 (T000 0100 || 0000 0001, 1111 1111)

		assertEquals(new Integer(2), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("84 01 FF"))), resolver, builder));
				// Dec: 1025 -- Hex result: 04 01 (xT00 0100, 0000 0001 || 1111 1111 )

		assertEquals(new Integer(3), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("C4 01 02 03 FF"))), resolver, builder));
				// Dec: 262402 -- Hex result: 04 01 02 (xxT0 0100, 0000 0001, 0000 0010 | 0000 0011 || 1111 1111)

		assertEquals(new Integer(4), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("E4 01 02 03 FF"))), resolver, builder));
				// Dec: 67174915 -- Hex result: 04 01 02 03 (xxxT 0100, 0000 0001, 0000 0010, 0000 0011 || 1111 1111)

		assertEquals(new Integer(2), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("81 00 02 03 FF"))), resolver, builder));
				// Dec: 256 -- Hex result: 01 (xT00 0001, 0000 0000 | 0000 0010, 0000 00011 || 1111 1111)

		assertEquals(new Integer(2), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("81 02 03 FF"))), resolver, builder));
				// Dec: 258 -- Hex result: 01 (xT00 0001, 0000 0010 || 0000 0010, 0000 00011, 1111 1111)

		assertEquals(new Integer(1), 
				codec.decode(new DefaultBitBuffer(ByteBuffer.wrap(toByteArray("43 FF"))), resolver, builder));
				// Dec: 67 -- Hex result: 01 (T100 0011 || 1111 1111)		

		verify(metadata, settings, builder);
	}
}
