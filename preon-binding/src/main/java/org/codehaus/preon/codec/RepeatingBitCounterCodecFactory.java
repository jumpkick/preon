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
import org.codehaus.preon.el.Expressions;

import nl.flotsam.pecia.Documenter;
import nl.flotsam.pecia.ParaContents;
import nl.flotsam.pecia.SimpleContents;

import org.codehaus.preon.*;
import org.codehaus.preon.annotation.BoundRepeatingBitCounter;
import org.codehaus.preon.buffer.BitBuffer;
import org.codehaus.preon.channel.BitChannel;

import java.io.IOException;
import java.lang.reflect.AnnotatedElement;


/**
 * A {@link CodecFactory} capable of creating {@link Codec Codecs} that deal with repeating bit counters.
 *
 * @author Garth Dahlstrom, 2Keys Corporation
 */
public class RepeatingBitCounterCodecFactory implements CodecFactory {

	/*
	 * (non-Javadoc)
	 * @see org.codehaus.preon.CodecFactory#create(java.lang.reflect.AnnotatedElement, java.lang.Class, org.codehaus.preon.ResolverContext)
	 */

	@SuppressWarnings("unchecked")
	public <T> Codec<T> create(AnnotatedElement metadata, Class<T> type,
			ResolverContext context) {
		if (metadata == null || metadata.isAnnotationPresent(BoundRepeatingBitCounter.class)) {
			if (Integer.class.equals(type)) {
				BoundRepeatingBitCounter settings = null;
				if (metadata != null) {
					settings = metadata.getAnnotation(BoundRepeatingBitCounter.class);
				}

				Byte terminateBit = 0;
				Expression<Integer, Resolver> sizeExpr = null;
				if (settings != null) {
					if (settings.maxCount() != null && !settings.maxCount().equals("")) {
						sizeExpr = Expressions.createInteger(context, settings.maxCount());
					}
					terminateBit = settings.terminateBit();
				}

				return (Codec<T>) new RepeatingBitCounterCodec(
						terminateBit,
						sizeExpr);
			} else {
				// System.err.println("RepeatingBitCounterCodecFactory.create 1 -- (should never see this)");
				return null;
			}
		} else {
			// System.err.println("RepeatingBitCounterCodecFactory.create 2 -- (should never see this)");
			return null;
		}
	}

	private static class RepeatingBitCounterCodec implements Codec<Integer> {

		private Integer count = new Integer(0);
		private byte terminateBit;
		protected Expression<Integer, Resolver> maxCountExpr;

		public RepeatingBitCounterCodec(byte terminateBit, Expression<Integer, Resolver> maxCountExpr) {
			this.terminateBit = terminateBit;
			this.maxCountExpr = maxCountExpr;
		}

		public Integer decode(BitBuffer buffer, Resolver resolver,
				Builder builder) throws DecodingException {
			int size = 0;
			if (this.maxCountExpr != null) {
				size = ((Number) (this.maxCountExpr.eval(resolver))).intValue();
			}

			count = 0;
			long readBit = terminateBit;
			do {
				readBit = buffer.readBits(1);
				count++;
			} while (readBit != terminateBit && (size <= 0 || count < size));

			return count;
		}


		public void encode(Integer value, BitChannel channel, Resolver resolver) throws IOException {
			// FIXME: no way to encode because context of buffer to be encoded doesn't exist in this scope :(
			channel.write(false);
		}

		public CodecDescriptor getCodecDescriptor() {
			return new CodecDescriptor() {

				public <T extends SimpleContents<?>> Documenter<T> details(
						String bufferReference) {
					return new Documenter<T>() {
						public void document(T target) {
						}
					};
				}

				public String getTitle() {
					return null;
				}

				public <T extends ParaContents<?>> Documenter<T> reference(
						final Adjective adjective, boolean startWithCapital) {
					return new Documenter<T>() {
						public void document(T target) {
							target.text(adjective == Adjective.A ? "a " : "the ");
							target.text("repeating bit counter");
						}
					};
				}

				public boolean requiresDedicatedSection() {
					return false;
				}

				public <T extends ParaContents<?>> Documenter<T> summary() {
					return new Documenter<T>() {
						public void document(T target) {
							target.text("a bit counter that increments until a terminateBit value is encountered or the counter reaches maxCount: ");
							target.text("i.e. 110|11111 -> 3, where terminateBit = 0, no maxCount provided (unlimited)");
						}
					};
				}

			};
		}

		public Expression<Integer, Resolver> getSize() {
			return maxCountExpr;
		}

		public Class<?> getType() {
			return Integer.class;
		}

		public Class<?>[] getTypes() {
			return new Class[]{Integer.class};
		}

	}

}
