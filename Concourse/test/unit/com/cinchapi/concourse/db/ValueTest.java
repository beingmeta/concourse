/*
 * This project is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This project is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this project. If not, see <http://www.gnu.org/licenses/>.
 */
package com.cinchapi.concourse.db;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cinchapi.concourse.util.Mocks;
import com.cinchapi.concourse.util.Timer;
import com.cinchapi.util.AtomicClock;
import com.cinchapi.util.RandomString;

import junit.framework.TestCase;

/**
 * Unit tests for {@link Value}.
 * 
 * @author jnelson
 */
public class ValueTest extends TestCase {

	private static AtomicClock clock = new AtomicClock();
	private static Random rand = new Random();
	private static RandomString strand = new RandomString();
	private static final Logger log = LoggerFactory.getLogger(ValueTest.class);

	@Test
	public void testCompare() {
		// default compare should compare time in descending order
		long t1 = clock.time();
		long t2 = clock.time();
		Value a = Value.forStorage(getRandomValue(), t1);
		Value b = Value.forStorage(getRandomValue(), t1);
		Value c = Value.forStorage(getRandomValue(), t2);
		assertTrue(a.compareTo(b) == 0);
		assertTrue(a.compareTo(c) > 0);
		assertTrue(c.compareTo(a) < 0);
		assertTrue(c.compareTo(b) < 0);

		// a comparative value should always be equal to an additive one with
		// the same raw value
		Value d = Value.notForStorage(a.getQuantity());
		assertTrue(d.compareTo(a) == 0);

		// a comparative value should always be greater than an additive value
		// with a different raw value
		assertTrue(d.compareTo(b) > 0);
		assertTrue(b.compareTo(d) < 0);
		assertTrue(d.compareTo(c) > 0);
		assertTrue(c.compareTo(d) < 0);

		// TODO logical number comparison should work regardless of type
	}

	public void testGetBytes() {
		Value v = Mocks.getValueForStorage();
		Value w = Value.fromByteSequence(ByteBuffer.wrap(v.getBytes()));
		assertEquals(w, v);
		assertEquals(w.getTimestamp(), v.getTimestamp()); // equality is only
															// based on quantity
															// and type, so i'm
															// checking the
															// timestamp
															// explicitly
	}

	public void testBenchmark() throws IOException {
		log.info("Running testBenchmark");
		NumberFormat format = NumberFormat.getNumberInstance();
		format.setGroupingUsed(true);
		
		//Test write to disk time
		int size = 100000;
		TimeUnit unit = TimeUnit.MILLISECONDS;
		
		Value[] values = new Value[size];
		long numBytes = 0;
		log.info("Creating {} Values...", format.format(size));
		for (int i = 0; i < size; i++) {
			Value value = Value.forStorage(getRandomValue());
			numBytes += value.size();
			values[i] = value;
		}

		String filePath = "test/value_test_benchmark.tst";
		RandomAccessFile file = new RandomAccessFile(filePath, "rw");
		Timer t = new Timer();
		t.start();
		log.info("Writing {} total BYTES to {}...", format.format(numBytes), filePath);
		for (int i = 0; i < size; i++) {
			Value value = values[i];
			value.writeTo(file.getChannel());
		}
		long elapsed = t.stop(unit);
		long bytesPerUnit = numBytes / elapsed;
		
		log.info("Total write time was {} {} with {} bytes written per {}",
				format.format(elapsed), unit, format.format(bytesPerUnit),
				unit.toString().substring(0, unit.toString().length() - 1));
	}

	private Object getRandomValue() {
		int seed = rand.nextInt();
		if(seed % 5 == 0) {
			return getRandomValueBoolean();
		}
		else if(seed % 2 == 0) {
			return getRandomValueNumber();
		}
		else {
			return getRandomValueString();
		}
	}

	private Boolean getRandomValueBoolean() {
		int seed = rand.nextInt();
		if(seed % 2 == 0) {
			return true;
		}
		else {
			return false;
		}
	}

	private Number getRandomValueNumber() {
		int seed = rand.nextInt();
		if(seed % 5 == 0) {
			return rand.nextFloat();
		}
		else if(seed % 4 == 0) {
			return rand.nextDouble();
		}
		else if(seed % 3 == 0) {
			return rand.nextLong();
		}
		else {
			return rand.nextInt();
		}
	}

	private String getRandomValueString() {
		return strand.nextStringAllowDigits();
	}

}
