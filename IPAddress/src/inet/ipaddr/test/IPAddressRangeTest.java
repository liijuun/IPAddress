/*
 * Copyright 2017 Sean C Foley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     or at
 *     https://github.com/seancfoley/IPAddress/blob/master/LICENSE
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package inet.ipaddr.test;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import inet.ipaddr.Address;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.AddressStringParameters.RangeParameters;
import inet.ipaddr.AddressTypeException;
import inet.ipaddr.HostIdentifierString;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSegment;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IPAddressStringParameters;
import inet.ipaddr.ipv4.IPv4Address;
import inet.ipaddr.ipv4.IPv4AddressSection;


public class IPAddressRangeTest extends IPAddressTest {
	
	static final IPAddressStringParameters WILDCARD_AND_RANGE_ADDRESS_OPTIONS = ADDRESS_OPTIONS.toBuilder().allowAll(true).setRangeOptions(RangeParameters.WILDCARD_AND_RANGE).toParams();
	static final IPAddressStringParameters WILDCARD_ONLY_ADDRESS_OPTIONS = WILDCARD_AND_RANGE_ADDRESS_OPTIONS.toBuilder().setRangeOptions(RangeParameters.WILDCARD_ONLY).toParams();
	private static final IPAddressStringParameters NO_RANGE_ADDRESS_OPTIONS = WILDCARD_AND_RANGE_ADDRESS_OPTIONS.toBuilder().setRangeOptions(RangeParameters.NO_RANGE).toParams();
	
	private static final IPAddressStringParameters INET_ATON_WILDCARD_OPTS = INET_ATON_WILDCARD_AND_RANGE_OPTIONS.toBuilder().setRangeOptions(RangeParameters.WILDCARD_ONLY).toParams();
	private static IPAddressStringParameters optionsCache[][] = new IPAddressStringParameters[3][3];

	IPAddressRangeTest(AddressCreator creator) {
		super(creator);
	}
	
	void testIPv4Strings(IPAddressString w, IPAddress ipAddr, String normalizedString, String normalizedWildcardString, String sqlString, String fullString, String octalString, String hexString, String reverseDNSString,
			String singleHex,
			String singleOctal) {
		testStrings(w, ipAddr, normalizedString, normalizedWildcardString, normalizedWildcardString, sqlString, fullString, 
				normalizedString, normalizedString, normalizedWildcardString, normalizedString, normalizedWildcardString, reverseDNSString, normalizedString,
				singleHex, singleOctal);
	
		//now test some IPv4-only strings
		testIPv4OnlyStrings(w, (IPv4Address) ipAddr, octalString, hexString);
		testInetAtonCombos(w, (IPv4Address) ipAddr);
	}
	
	private void testIPv4OnlyStrings(IPAddressString w, IPv4Address ipAddr, String octalString, String hexString) {
		String oct = ipAddr.toInetAtonString(IPv4Address.inet_aton_radix.OCTAL);
		String hex = ipAddr.toInetAtonString(IPv4Address.inet_aton_radix.HEX);
		
		boolean octMatch = oct.equals(octalString);
		if(!octMatch) {
			addFailure(new Failure("failed expected: " + octalString + " actual: " + oct, w));
		} else {
			boolean hexMatch = hex.equals(hexString);
			if(!hexMatch) {
				addFailure(new Failure("failed expected: " + hexString + " actual: " + hex, w));
			}
		}
		incrementTestCount();
	}
	
	void testInetAtonCombos(IPAddressString w, IPv4Address ipAddr) {
		for(IPv4Address.inet_aton_radix radix : IPv4Address.inet_aton_radix.values()) {
			for(int i = 0; i < IPv4Address.SEGMENT_COUNT; i++) {
				try {
					String str = ipAddr.toInetAtonString(radix, i);
					IPAddressString parsed = new IPAddressString(str, INET_ATON_WILDCARD_AND_RANGE_OPTIONS);
					try {
						IPAddress parsedValue = parsed.getAddress();
						if(!ipAddr.equals(parsedValue)) {
							addFailure(new Failure("failed expected: " + ipAddr + " actual: " + parsedValue, w));
						} else {
							int pos;
							int count = 0;
							while ((pos = str.indexOf(IPv4Address.SEGMENT_SEPARATOR)) >= 0) {
								str = str.substring(pos + 1);
								count++;
							}
							if(IPv4Address.SEGMENT_COUNT - 1 - i != count) {
								addFailure(new Failure("failed expected separator count: " + (IPv4Address.SEGMENT_COUNT - 1 - i) + " actual separator count: " + count, w));
							}
						}
					} catch(AddressTypeException e) {
						addFailure(new Failure("failed expected: " + ipAddr + " actual: " + e.getMessage(), w));
					}
				} catch(AddressTypeException e) {
					//verify this case: joining segments results in a joined segment that is not a contiguous range
					IPv4AddressSection section =  ipAddr.getSection();
					boolean verifiedIllegalJoin = false;
					for(int j = section.getSegmentCount() - i - 1; j < section.getSegmentCount() - 1; j++) {
						if(section.getSegment(j).isMultiple()) {
							for(j++; j < section.getSegmentCount(); j++) {
								if(!section.getSegment(j).isFullRange()) {
									verifiedIllegalJoin = true;
									break;
								}
							}
						}
					}
					if(!verifiedIllegalJoin) {
						addFailure(new Failure("failed expected: " + ipAddr + " actual: " + e.getMessage(), w));
					}
				}
				incrementTestCount();
			}
		}
	}

	@Override
	protected IPAddressString createInetAtonAddress(String x) {
		IPAddressStringParameters opts;
		if(x.indexOf(IPAddress.RANGE_SEPARATOR) != -1) {
			opts = INET_ATON_WILDCARD_AND_RANGE_OPTIONS;
		} else {
			opts = INET_ATON_WILDCARD_OPTS;
		}
		return createAddress(x, opts);
	}
	
	@Override
	protected IPAddressString createAddress(String x) {
		if(x.indexOf(IPAddress.RANGE_SEPARATOR) != -1) {
			return createAddress(x, WILDCARD_AND_RANGE_ADDRESS_OPTIONS);
		}
		return createAddress(x, WILDCARD_ONLY_ADDRESS_OPTIONS);
	}
	
	protected IPAddressString createAddress(String x, RangeParameters ipv4RangeOptions, RangeParameters ipv6RangeOptions) {
		IPAddressStringParameters validationOptions = getOpts(ipv4RangeOptions, ipv6RangeOptions);
		return createAddress(x, validationOptions);
	}
	
	private static IPAddressStringParameters getOpts(RangeParameters ipv4RangeOptions, RangeParameters ipv6RangeOptions) {
		int cacheIndex, subCacheIndex;
		if(ipv4RangeOptions.equals(RangeParameters.NO_RANGE)) {
			cacheIndex = 0;
		} else if(ipv4RangeOptions.equals(RangeParameters.WILDCARD_ONLY)) {
			cacheIndex = 1;
		} else if(ipv4RangeOptions.equals(RangeParameters.WILDCARD_AND_RANGE)) {
			cacheIndex = 2;
		} else {
			return WILDCARD_AND_RANGE_ADDRESS_OPTIONS.toBuilder().getIPv4AddressParametersBuilder().setRangeOptions(ipv4RangeOptions).getParentBuilder().
				getIPv6AddressParametersBuilder().setRangeOptions(ipv6RangeOptions).getParentBuilder().toParams();
		}
		if(ipv6RangeOptions.equals(RangeParameters.NO_RANGE)) {
			subCacheIndex = 0;
		} else if(ipv6RangeOptions.equals(RangeParameters.WILDCARD_ONLY)) {
			subCacheIndex = 1;
		} else if(ipv6RangeOptions.equals(RangeParameters.WILDCARD_AND_RANGE)) {
			subCacheIndex = 2;
		} else {
			return WILDCARD_AND_RANGE_ADDRESS_OPTIONS.toBuilder().getIPv4AddressParametersBuilder().setRangeOptions(ipv4RangeOptions).getParentBuilder().
				getIPv6AddressParametersBuilder().setRangeOptions(ipv6RangeOptions).getParentBuilder().toParams();
		}
		IPAddressStringParameters optionsSubCache[] = optionsCache[cacheIndex];
		IPAddressStringParameters res = optionsSubCache[subCacheIndex];
		if(res == null) {
			optionsSubCache[subCacheIndex] = res = WILDCARD_AND_RANGE_ADDRESS_OPTIONS.toBuilder().getIPv4AddressParametersBuilder().setRangeOptions(ipv4RangeOptions).getParentBuilder().
					getIPv6AddressParametersBuilder().setRangeOptions(ipv6RangeOptions).getParentBuilder().toParams();
		}
		return res;
	}
	
	private static IPAddressStringParameters getOpts(RangeParameters options) {
		if(options.equals(RangeParameters.NO_RANGE)) {
			return NO_RANGE_ADDRESS_OPTIONS;
		} else if(options.equals(RangeParameters.WILDCARD_ONLY)) {
			return WILDCARD_ONLY_ADDRESS_OPTIONS;
		} else if(options.equals(RangeParameters.WILDCARD_AND_RANGE)) {
			return WILDCARD_AND_RANGE_ADDRESS_OPTIONS;
		}
		return WILDCARD_AND_RANGE_ADDRESS_OPTIONS.toBuilder().setRangeOptions(options).toParams();
	}
	
	protected IPAddressString createAddress(String x, RangeParameters options) {
		return createAddress(x, getOpts(options));
	}
	
	void ipv4test(boolean pass, String x, RangeParameters ipv4RangeOptions, RangeParameters ipv6RangeOptions) {
		ipv4test(pass, x, false, ipv4RangeOptions, ipv6RangeOptions);
	}
	
	void ipv4test(boolean pass, String x, RangeParameters rangeOptions) {
		ipv4test(pass, x, false, rangeOptions);
	}
	
	void ipv4test(boolean pass, String x, boolean isZero, RangeParameters rangeOptions) {
		iptest(pass, x, isZero, false, true, rangeOptions);
	}
	
	void ipv4test(boolean pass, String x, boolean isZero, RangeParameters ipv4RangeOptions, RangeParameters ipv6RangeOptions) {
		iptest(pass, x, isZero, false, true, ipv4RangeOptions, ipv6RangeOptions);
	}
	
	void ipv6test(boolean pass, String x, RangeParameters options) {
		ipv6test(pass, x, false, options);
	}
	
	void ipv6test(boolean pass, String x, RangeParameters ipv4Options, RangeParameters ipv6Options) {
		ipv6test(pass, x, false, ipv4Options, ipv6Options);
	}
	
	void ipv6test(boolean pass, String x, boolean isZero, RangeParameters options) {
		iptest(pass, x, isZero, false, false, options);
	}
	
	void ipv6test(boolean pass, String x, boolean isZero, RangeParameters ipv4Options, RangeParameters ipv6Options) {
		iptest(pass, x, isZero, false, false, ipv4Options, ipv6Options);
	}
	
	void iptest(boolean pass, String x, boolean isZero, boolean notBoth, boolean ipv4Test, RangeParameters ipv4RangeOptions, RangeParameters ipv6RangeOptions) {
		IPAddressString addr = createAddress(x, ipv4RangeOptions, ipv6RangeOptions);
		if(iptest(pass, addr, isZero, notBoth, ipv4Test)) {
			//do it a second time to test the caching
			iptest(pass, addr, isZero, notBoth, ipv4Test);
		}
	}
	
	void iptest(boolean pass, String x, boolean isZero, boolean notBoth, boolean ipv4Test, RangeParameters rangeOptions) {
		IPAddressString addr = createAddress(x, rangeOptions);
		if(iptest(pass, addr, isZero, notBoth, ipv4Test)) {
			//do it a second time to test the caching
			iptest(pass, addr, isZero, notBoth, ipv4Test);
		}
	}
	
	@Override
	void ipv6testWithZone(int pass, String x) {
		return;
	}
	
	@Override
	void ipv6testWithZone(boolean pass, String x) {
		return;
	}
	
	@Override
	boolean testBytes(IPAddress origAddr) {
		boolean failed = false;
		if(origAddr.isMultiple()) {
			try {
				origAddr.getBytes();
				//addFailure(new Failure("wildcard bytes on addr ", origAddr)); now this just gets the lower bytes
				//failed = true;
			} catch(AddressTypeException e) {
				failed = true;
				//pass
				//wild addresses have no bytes
			}
		} else {
			failed = !super.testBytes(origAddr);
		}
		return !failed;
	}
	
	@Override
	void testMaskBytes(String cidr2, IPAddressString w2)
			throws AddressStringException {
		IPAddress addr = w2.toAddress();
		testBytes(addr);
	}
	
	void testCount(String original, int number, RangeParameters rangeOptions) {
		IPAddressString w = createAddress(original, rangeOptions);
		testCount(this, w, number);
	}
	
	void testCount(String original, int number) {
		IPAddressString w = createAddress(original);
		testCount(this, w, number);
	}
	
	static void testCount(TestBase testBase, HostIdentifierString w, int number) {
		Address val = w.getAddress();
		BigInteger count = val.getCount();
		if(!count.equals(BigInteger.valueOf(number))) {
			testBase.addFailure(new Failure("count was " + count, w));
		} else {
			Iterator<? extends Address> addrIterator = val.iterator();
			int counter = 0;
			Set<Address> set = new HashSet<Address>();
			Address next = null;
			while(addrIterator.hasNext()) {
				next = addrIterator.next();
				if(counter == 0) {
					if(!next.equals(val.getLower())) {
						testBase.addFailure(new Failure("lowest: " + val.getLower(), next));
					}
				}
				set.add(next);
				counter++;
			}
			if(set.size() != number || counter != number) {
				testBase.addFailure(new Failure("set count was " + set.size() + " instead of expected " + number, w));
			} else {
				if(!next.equals(val.getUpper())) {
					testBase.addFailure(new Failure("highest: " + val.getUpper(), next));
				} else {
					if(counter == 1 && !val.getUpper().equals(val.getLower())) {
						testBase.addFailure(new Failure("highest: " + val.getUpper() + " lowest: " + val.getLower(), next));
					}
				}
			}
		}
		testBase.incrementTestCount();
	}
	
	void testIPv4Wildcarded(String original, int bits, String expected, String expectedSQL) {
		testWildcarded(original, bits, expected, expected, expected, expected, expectedSQL);
	}
	
	void testIPv6Wildcarded(String original, int bits, String expectedSubnet, String expectedNormalizedCompressedCanonical, String expectedSQL) {
		String all = expectedNormalizedCompressedCanonical;
		testWildcarded(original, bits, expectedSubnet, all, all, all, expectedSQL);
	}
	
	void testWildcarded(String original, int bits, String expectedSubnet, String expectedNormalized, String expectedCanonical, String expectedCompressed, String expectedSQL) {
		IPAddressString w = createAddress(original);
		IPAddress addr = w.getAddress();
		addr = addr.applyPrefixLength(bits);
		String string = addr.toCompressedWildcardString();
		if(!string.equals(expectedCompressed)) {
			addFailure(new Failure("failed expected: " + expectedCompressed + " actual: " + string, w));
		} else {
			IPAddressString w2 = createAddress(original + '/' + bits);
			IPAddress addr2 = w2.getAddress();
			string = addr2.toCompressedWildcardString();
			if(!string.equals(expectedCompressed)) {
				addFailure(new Failure("failed expected: " + expectedCompressed + " actual: " + string, w));
			} else {
				string = addr.toNormalizedWildcardString();
				if(!string.equals(expectedNormalized)) {
					addFailure(new Failure("failed expected: " + expectedNormalized + " actual: " + string, w));
				} else {
					string = addr2.toNormalizedWildcardString();
					if(!string.equals(expectedNormalized)) {
						addFailure(new Failure("failed expected: " + expectedNormalized + " actual: " + string, w));
					} else {
						string = addr.toCanonicalWildcardString();
						if(!string.equals(expectedCanonical)) {
							addFailure(new Failure("failed expected: " + expectedCanonical + " actual: " + string, w));
						} else {
							string = addr.toSubnetString();
							if(!string.equals(expectedSubnet)) {
								addFailure(new Failure("failed expected: " + expectedSubnet + " actual: " + string, w));
							} else {
								string = addr2.toSubnetString();
								if(!string.equals(expectedSubnet)) {
									addFailure(new Failure("failed expected: " + expectedSubnet + " actual: " + string, w));
								} else {
									string = addr2.toSQLWildcardString();
									if(!string.equals(expectedSQL)) {
										addFailure(new Failure("failed expected: " + expectedSQL + " actual: " + string, w));
									}
								}
							}
						}
					}
				}
			}
		}
		incrementTestCount();
	}
	
	void testIPv4Strings(String addr, String normalizedString, String normalizedWildcardString, String sqlString, String fullString, String octalString, String hexString, String reverseDNSString, String singleHex, String singleOctal) {
		IPAddressString w = createAddress(addr);
		IPAddress ipAddr = w.getAddress();
		//createList(w);
		testIPv4Strings(w, ipAddr, normalizedString, normalizedWildcardString, sqlString, fullString, octalString, hexString, reverseDNSString, singleHex, singleOctal);
	}
	
	void createList(IPAddressString str) {}
	
	void testIPv6Strings(String addr, 
			String normalizedString,
			String normalizedWildcardString, 
			String canonicalWildcardString, 
			String sqlString, 
			String fullString,
			String compressedString,
			String canonicalString,
			String subnetString,
			String compressedWildcardString,
			String mixedStringNoCompressMixed,
			String mixedStringNoCompressHost,
			String mixedStringCompressCoveredHost,
			String mixedString,
			String reverseDNSString,
			String uncHostString,
			String base85String,
			String singleHex,
			String singleOctal) {
		IPAddressString w = createAddress(addr);
		IPAddress ipAddr = w.getAddress();
		
		//createList(w);
		
		testIPv6Strings(w,
				ipAddr,
				normalizedString,
				normalizedWildcardString,
				canonicalWildcardString,
				sqlString, 
				fullString,
				compressedString,
				canonicalString,
				subnetString,
				compressedWildcardString,
				mixedStringNoCompressMixed,
				mixedStringNoCompressHost,
				mixedStringCompressCoveredHost,
				mixedString,
				reverseDNSString,
				uncHostString,
				base85String,
				singleHex,
				singleOctal);
	}
	
	void testTree(String start, String parents[]) {
		IPAddressString str = createAddress(start, WILDCARD_AND_RANGE_ADDRESS_OPTIONS);
		IPAddressString originaLabelStr = str;
		IPAddressString labelStr = str;
		if(!str.isPrefixed()) { 
			IPAddress address = str.getAddress();
			//convert 1.2.3.* to 1.2.3.*/24 which is needed by adjustPrefixBySegment
			address = address.toPrefixedEquivalent();
			str = address.toAddressString();
		}
		
		IPAddressString original = str;
		
		int i = 0;
		do {
			String label = getLabel(labelStr);
			String expected = parents[i];
			if(!label.equals(expected)) {
				addFailure(new Failure("failed expected: " + expected + " actual: " + label, str));
				break;
			}
			labelStr = str = str.adjustPrefixBySegment(false);
			i++;
		} while(str != null);
		
		
		//now do the same thing but use the IPAddress objects instead
		labelStr = originaLabelStr;
		str = original;
		i = 0;
		do {
			String label = getLabel(labelStr);
			String expected = parents[i];
			if(!label.equals(expected)) {
				addFailure(new Failure("failed expected: " + expected + " actual: " + label, str));
				break;
			}
			labelStr = str = str.getAddress().adjustPrefixBySegment(false).toAddressString();
			i++;
		} while(str.getNetworkPrefixLength() != 0); //when network prefix is 0, IPAddress.adjustPrefixBySegment() returns the same address
		incrementTestCount();
	}
	
	static String getLabel(IPAddressString addressString) {
		IPAddress address = addressString.getAddress();
		if(address == null) {
			return addressString.toString();
		}
		return address.toSubnetString();
	}
	
	void testTrees() {
		testTree("1.2.3.4", new String[] {
				"1.2.3.4",
				"1.2.3.*",
				"1.2.*.*",
				"1.*.*.*",
				"*.*.*.*",
				"*"
		});
		
		testTree("1.2.3.*", new String[] {
				"1.2.3.*",
				"1.2.*.*",
				"1.*.*.*",
				"*.*.*.*",
				"*"
		});
		
		testTree("1.2.*.*", new String[] {
				"1.2.*.*",
				"1.*.*.*",
				"*.*.*.*",
				"*"
		});
		
		testTree("1.2.3.4/28", new String[] {
				"1.2.3.0-15",
				"1.2.3.*",
				"1.2.*.*",
				"1.*.*.*",
				"*.*.*.*",
				"*"
		});
		testTree("1.2.3.4/17", new String[] {
				"1.2.0-127.*",
				"1.2.*.*",
				"1.*.*.*",
				"*.*.*.*",
				"*"
		});
		testTree("a:b:c:d:e:f:a:b", new String[] {
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a::/112",
				"a:b:c:d:e:f::/96",
				"a:b:c:d:e::/80",
				"a:b:c:d::/64",
				"a:b:c::/48",
				"a:b::/32",
				"a::/16",
				"::/0",
				"*"
		});
		testTree("a:b:c:d:e:f:a:b/97", new String[] {
				"a:b:c:d:e:f::/97",
				"a:b:c:d:e:f::/96",
				"a:b:c:d:e::/80",
				"a:b:c:d::/64",
				"a:b:c::/48",
				"a:b::/32",
				"a::/16",
				"::/0",
				"*"
		});
		testTree("a:b:c:d:e:f:ffff:b/97", new String[] {
				"a:b:c:d:e:f:8000::/97",
				"a:b:c:d:e:f::/96",
				"a:b:c:d:e::/80",
				"a:b:c:d::/64",
				"a:b:c::/48",
				"a:b::/32",
				"a::/16",
				"::/0",
				"*"
		});
		testTree("a:b:c:d:e:f:a:b/96", new String[] {
				"a:b:c:d:e:f::/96",
				"a:b:c:d:e::/80",
				"a:b:c:d::/64",
				"a:b:c::/48",
				"a:b::/32",
				"a::/16",
				"::/0",
				"*"
		});
		testTree("a:b:c:d::a:b", new String[] {
				"a:b:c:d::a:b",
				"a:b:c:d:0:0:a::/112",
				"a:b:c:d::/96",
				"a:b:c:d::/80",
				"a:b:c:d::/64",
				"a:b:c::/48",
				"a:b::/32",
				"a::/16",
				"::/0",
				"*"
		});
		testTree("::c:d:e:f:a:b", new String[] {
				"::c:d:e:f:a:b",
				"0:0:c:d:e:f:a::/112",
				"0:0:c:d:e:f::/96",
				"0:0:c:d:e::/80",
				"0:0:c:d::/64",
				"0:0:c::/48",
				"::/32",
				"::/16",
				"::/0",
				"*"
		});
	}
	
	void testStrings() {
		testIPv4Strings("1.2.3.4", "1.2.3.4", "1.2.3.4", "1.2.3.4", "001.002.003.004", "01.02.03.04", "0x1.0x2.0x3.0x4", "4.3.2.1.in-addr.arpa", "0x01020304", "000100401404");
		testIPv4Strings("1.2.3.4/16", "1.2.0.0/16", "1.2.*.*", "1.2.%.%", "001.002.000.000/16", "01.02.00.00/16", "0x1.0x2.0x0.0x0/16", "*.*.2.1.in-addr.arpa", "0x01020000-0x0102ffff", "000100400000-000100577777");
		testIPv4Strings("1.2.*.*", "1.2.*.*", "1.2.*.*", "1.2.%.%", "001.002.000-255.000-255", "01.02.*.*", "0x1.0x2.*.*", "*.*.2.1.in-addr.arpa", "0x01020000-0x0102ffff", "000100400000-000100577777");//note that wildcards are never converted to CIDR.  //for CIDR call toCIDREquivalent() or getMinPrefix() or getMaskPrefixLength()
		testIPv4Strings("1.2.*", "1.2.*.*", "1.2.*.*", "1.2.%.%", "001.002.000-255.000-255", "01.02.*.*", "0x1.0x2.*.*", "*.*.2.1.in-addr.arpa", "0x01020000-0x0102ffff", "000100400000-000100577777");
		testIPv4Strings("1.2.*.*/16", "1.2.0.0/16", "1.2.*.*", "1.2.%.%", "001.002.000.000/16", "01.02.00.00/16", "0x1.0x2.0x0.0x0/16", "*.*.2.1.in-addr.arpa", "0x01020000-0x0102ffff", "000100400000-000100577777");
		testIPv4Strings("1.2.*/16", "1.2.0.0/16", "1.2.*.*", "1.2.%.%", "001.002.000.000/16", "01.02.00.00/16", "0x1.0x2.0x0.0x0/16", "*.*.2.1.in-addr.arpa", "0x01020000-0x0102ffff", "000100400000-000100577777");
		testIPv4Strings("1.*.*/16",  "1.*.0.0/16", "1.*.*.*", "1.%.%.%", "001.000-255.000.000/16",  "01.*.00.00/16",  "0x1.*.0x0.0x0/16", "*.*.*.1.in-addr.arpa", "0x01000000-0x01ffffff", "000100000000-000177777777");
		
		testIPv4Strings("0.0.0.0", "0.0.0.0", "0.0.0.0", "0.0.0.0", "000.000.000.000", "00.00.00.00", "0x0.0x0.0x0.0x0", "0.0.0.0.in-addr.arpa", "0x00000000", "000000000000");
		testIPv4Strings("9.63.127.254", "9.63.127.254", "9.63.127.254", "9.63.127.254", "009.063.127.254", "011.077.0177.0376", "0x9.0x3f.0x7f.0xfe", "254.127.63.9.in-addr.arpa", "0x093f7ffe", "001117677776");
		testIPv4Strings("9.63.127.254/16", "9.63.0.0/16", "9.63.*.*", "9.63.%.%", "009.063.000.000/16", "011.077.00.00/16", "0x9.0x3f.0x0.0x0/16", "*.*.63.9.in-addr.arpa", "0x093f0000-0x093fffff", "001117600000-001117777777");
		testIPv4Strings("9.63.*.*", "9.63.*.*", "9.63.*.*", "9.63.%.%", "009.063.000-255.000-255", "011.077.*.*", "0x9.0x3f.*.*", "*.*.63.9.in-addr.arpa", "0x093f0000-0x093fffff", "001117600000-001117777777");//note that wildcards are never converted to CIDR.  //for CIDR call toCIDREquivalent() or getMinPrefix() or getMaskPrefixLength()
		testIPv4Strings("9.63.*", "9.63.*.*", "9.63.*.*", "9.63.%.%", "009.063.000-255.000-255", "011.077.*.*", "0x9.0x3f.*.*", "*.*.63.9.in-addr.arpa", "0x093f0000-0x093fffff", "001117600000-001117777777");
		testIPv4Strings("9.63.*.*/16", "9.63.0.0/16", "9.63.*.*", "9.63.%.%", "009.063.000.000/16", "011.077.00.00/16", "0x9.0x3f.0x0.0x0/16", "*.*.63.9.in-addr.arpa", "0x093f0000-0x093fffff", "001117600000-001117777777");
		testIPv4Strings("9.63.*/16", "9.63.0.0/16", "9.63.*.*", "9.63.%.%", "009.063.000.000/16", "011.077.00.00/16", "0x9.0x3f.0x0.0x0/16", "*.*.63.9.in-addr.arpa", "0x093f0000-0x093fffff", "001117600000-001117777777");
		testIPv4Strings("9.*.*/16",  "9.*.0.0/16", "9.*.*.*", "9.%.%.%", "009.000-255.000.000/16", "011.*.00.00/16", "0x9.*.0x0.0x0/16", "*.*.*.9.in-addr.arpa", "0x09000000-0x09ffffff", "001100000000-001177777777"); 
		
		testIPv4Strings("1.2.3.250-255", "1.2.3.250-255", "1.2.3.250-255", "1.2.3.25_", "001.002.003.250-255", "01.02.03.0372-0377", "0x1.0x2.0x3.0xfa-0xff", "250-255.3.2.1.in-addr.arpa", "0x010203fa-0x010203ff", "000100401772-000100401777");
		testIPv4Strings("1.2.3.200-255", "1.2.3.200-255", "1.2.3.200-255", "1.2.3.2__", "001.002.003.200-255", "01.02.03.0310-0377", "0x1.0x2.0x3.0xc8-0xff", "200-255.3.2.1.in-addr.arpa", "0x010203c8-0x010203ff", "000100401710-000100401777");
		testIPv4Strings("1.2.3.100-199", "1.2.3.100-199", "1.2.3.100-199", "1.2.3.1__", "001.002.003.100-199", "01.02.03.0144-0307", "0x1.0x2.0x3.0x64-0xc7", "100-199.3.2.1.in-addr.arpa", "0x01020364-0x010203c7", "000100401544-000100401707");
		testIPv4Strings("100-199.2.3.100-199", "100-199.2.3.100-199", "100-199.2.3.100-199", "1__.2.3.1__", "100-199.002.003.100-199", "0144-0307.02.03.0144-0307", "0x64-0xc7.0x2.0x3.0x64-0xc7", "100-199.3.2.100-199.in-addr.arpa", null, null);
		testIPv4Strings("100-199.2.3.100-198", "100-199.2.3.100-198", "100-199.2.3.100-198", "1__.2.3.100-198", "100-199.002.003.100-198", "0144-0307.02.03.0144-0306", "0x64-0xc7.0x2.0x3.0x64-0xc6", "100-198.3.2.100-199.in-addr.arpa", null, null);
		testIPv4Strings("1.2.3.0-99", "1.2.3.0-99", "1.2.3.0-99", "1.2.3.0-99", "001.002.003.000-099", "01.02.03.00-0143", "0x1.0x2.0x3.0x0-0x63", "0-99.3.2.1.in-addr.arpa", "0x01020300-0x01020363", "000100401400-000100401543");
		testIPv4Strings("1.2.3.100-155", "1.2.3.100-155", "1.2.3.100-155", "1.2.3.100-155", "001.002.003.100-155", "01.02.03.0144-0233", "0x1.0x2.0x3.0x64-0x9b", "100-155.3.2.1.in-addr.arpa", "0x01020364-0x0102039b", "000100401544-000100401633");
		testIPv4Strings("1.2.3.100-255", "1.2.3.100-255", "1.2.3.100-255", "1.2.3.100-255", "001.002.003.100-255", "01.02.03.0144-0377", "0x1.0x2.0x3.0x64-0xff", "100-255.3.2.1.in-addr.arpa", "0x01020364-0x010203ff", "000100401544-000100401777");
		testIPv4Strings("1.129-254.5.5/12", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "1.128-240.0.0/12" : "1.128-255.0.0/12", "1.128-255.*.*", "1.128-255.%.%", "001.128-240.000.000/12", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "01.0200-0360.00.00/12" : "01.0200-0377.00.00/12", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0x1.0x80-0xf0.0x0.0x0/12" : "0x1.0x80-0xff.0x0.0x0/12", "*.*.128-255.1.in-addr.arpa", "0x01800000-0x01ffffff", "000140000000-000177777777");
		testIPv4Strings("1.2__.5.5/14", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "1.200-252.0.0/14" : "1.200-255.0.0/14", "1.200-255.*.*", "1.2__.%.%", "001.200-252.000.000/14", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "01.0310-0374.00.00/14" : "01.0310-0377.00.00/14", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0x1.0xc8-0xfc.0x0.0x0/14" : "0x1.0xc8-0xff.0x0.0x0/14", "*.*.200-255.1.in-addr.arpa", "0x01c80000-0x01ffffff", "000162000000-000177777777");
		testIPv4Strings("1.*.5.5/12", "1.*.0.0/12", "1.*.*.*", "1.%.%.%", "001.000-240.000.000/12", "01.*.00.00/12", "0x1.*.0x0.0x0/12", "*.*.*.1.in-addr.arpa", "0x01000000-0x01ffffff", "000100000000-000177777777");


		testIPv6Strings("::",
				"0:0:0:0:0:0:0:0",
				"0:0:0:0:0:0:0:0",
				"::",
				"0:0:0:0:0:0:0:0",
				"0000:0000:0000:0000:0000:0000:0000:0000",
				"::",
				"::",
				"::",
				"::",
				"::0.0.0.0",
				"::",
				"::",
				"::",
				"0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-0-0-0-0-0-0.ipv6-literal.net",
				"00000000000000000000",
				"0x00000000000000000000000000000000",
				"00000000000000000000000000000000000000000000");
		
		testIPv6Strings("::2",
				"0:0:0:0:0:0:0:2",
				"0:0:0:0:0:0:0:2",
				"::2",
				"0:0:0:0:0:0:0:2",
				"0000:0000:0000:0000:0000:0000:0000:0002",
				"::2",
				"::2",
				"::2",
				"::2",
				"::0.0.0.2",
				"::0.0.0.2",
				"::0.0.0.2",
				"::0.0.0.2",
				"2.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-0-0-0-0-0-2.ipv6-literal.net",
				"00000000000000000002",
				"0x00000000000000000000000000000002",
				"00000000000000000000000000000000000000000002");
		
		testIPv6Strings("::7fff:ffff:ffff:ffff",
				"0:0:0:0:7fff:ffff:ffff:ffff",
				"0:0:0:0:7fff:ffff:ffff:ffff",
				"::7fff:ffff:ffff:ffff",
				"0:0:0:0:7fff:ffff:ffff:ffff",
				"0000:0000:0000:0000:7fff:ffff:ffff:ffff",
				"::7fff:ffff:ffff:ffff",
				"::7fff:ffff:ffff:ffff",
				"::7fff:ffff:ffff:ffff",
				"::7fff:ffff:ffff:ffff",
				"::7fff:ffff:255.255.255.255",
				"::7fff:ffff:255.255.255.255",
				"::7fff:ffff:255.255.255.255",
				"::7fff:ffff:255.255.255.255",
				"f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.7.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-0-0-7fff-ffff-ffff-ffff.ipv6-literal.net",
				"0000000000d*-h_{Y}sg",
				"0x00000000000000007fffffffffffffff",
				"00000000000000000000000777777777777777777777");
		
		testIPv6Strings("0:0:0:1::",
				"0:0:0:1:0:0:0:0",
				"0:0:0:1:0:0:0:0",
				"0:0:0:1::",
				"0:0:0:1:0:0:0:0",
				"0000:0000:0000:0001:0000:0000:0000:0000",
				"0:0:0:1::",
				"0:0:0:1::",
				"0:0:0:1::",
				"0:0:0:1::",
				"::1:0:0:0.0.0.0",
				"0:0:0:1::",
				"0:0:0:1::",
				"0:0:0:1::",
				"0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-0-1-0-0-0-0.ipv6-literal.net",
				"0000000000_sw2=@*|O1",
				"0x00000000000000010000000000000000",
				"00000000000000000000002000000000000000000000");
		
		testIPv6Strings("::8fff:ffff:ffff:ffff",
				"0:0:0:0:8fff:ffff:ffff:ffff",
				"0:0:0:0:8fff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff",
				"0:0:0:0:8fff:ffff:ffff:ffff",
				"0000:0000:0000:0000:8fff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff",
				"::8fff:ffff:255.255.255.255",
				"::8fff:ffff:255.255.255.255",
				"::8fff:ffff:255.255.255.255",
				"::8fff:ffff:255.255.255.255",
				"f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.8.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-0-0-8fff-ffff-ffff-ffff.ipv6-literal.net",
				"0000000000i(`c)xypow",
				"0x00000000000000008fffffffffffffff",
				"00000000000000000000001077777777777777777777");
		
		testIPv6Strings("::8fff:ffff:ffff:ffff:ffff",
				"0:0:0:8fff:ffff:ffff:ffff:ffff",
				"0:0:0:8fff:ffff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff:ffff",
				"0:0:0:8fff:ffff:ffff:ffff:ffff",
				"0000:0000:0000:8fff:ffff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:ffff:ffff",
				"::8fff:ffff:ffff:255.255.255.255",
				"::8fff:ffff:ffff:255.255.255.255",
				"::8fff:ffff:ffff:255.255.255.255",
				"::8fff:ffff:ffff:255.255.255.255",
				"f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.f.8.0.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-0-8fff-ffff-ffff-ffff-ffff.ipv6-literal.net",
				"00000004&U-n{rbbza$w",
				"0x0000000000008fffffffffffffffffff",
				"00000000000000000217777777777777777777777777");
		
		testIPv6Strings("a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"000a:000b:000c:000d:000e:000f:000a:000b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:a:b",
				"a:b:c:d:e:f:0.10.0.11",
				"a:b:c:d:e:f:0.10.0.11",
				"a:b:c:d:e:f:0.10.0.11",
				"a:b:c:d:e:f:0.10.0.11",
				"b.0.0.0.a.0.0.0.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-e-f-a-b.ipv6-literal.net",
				"00|N0s0$ND2DCD&%D3QB",
				"0x000a000b000c000d000e000f000a000b",
				"00000240001300006000032000160000740002400013");
		testIPv6Strings("a:b:c:d:e:f:a:b/64",
				"a:b:c:d:0:0:0:0/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000:0000:0000:0000/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-0-0-0-0.ipv6-literal.net/64",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k/64",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("::c:d:e:f:a:b/64",
				"0:0:c:d:0:0:0:0/64",
				"0:0:c:d:*:*:*:*",
				"::c:d:*:*:*:*",
				"0:0:c:d:%:%:%:%",
				"0000:0000:000c:000d:0000:0000:0000:0000/64",
				"0:0:c:d::/64",
				"0:0:c:d::/64",
				"0:0:c:d::/64",
				"::c:d:*:*:*:*",
				"::c:d:0:0:0.0.0.0/64",
				"::c:d:0:0:0.0.0.0/64",
				"0:0:c:d::/64",
				"0:0:c:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-c-d-0-0-0-0.ipv6-literal.net/64",
				"0000001G~Ie?xF;x&)@P�0000001G~JZkWI!qp&GP/64",
				"0x00000000000c000d0000000000000000-0x00000000000c000dffffffffffffffff",
				"00000000000000006000032000000000000000000000-00000000000000006000033777777777777777777777");
		testIPv6Strings("::c:d:e:f:a:b",
				"0:0:c:d:e:f:a:b",
				"0:0:c:d:e:f:a:b",
				"::c:d:e:f:a:b",
				"0:0:c:d:e:f:a:b",
				"0000:0000:000c:000d:000e:000f:000a:000b",
				"::c:d:e:f:a:b",
				"::c:d:e:f:a:b",
				"::c:d:e:f:a:b",
				"::c:d:e:f:a:b",
				"::c:d:e:f:0.10.0.11",
				"::c:d:e:f:0.10.0.11",
				"::c:d:e:f:0.10.0.11",
				"::c:d:e:f:0.10.0.11",
				"b.0.0.0.a.0.0.0.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.0.0.0.0.ip6.arpa",
				"0-0-c-d-e-f-a-b.ipv6-literal.net",
				"0000001G~Ie^C9jXExx>",
				"0x00000000000c000d000e000f000a000b",
				"00000000000000006000032000160000740002400013");
		testIPv6Strings("a:b:c:d::",
				"a:b:c:d:0:0:0:0",
				"a:b:c:d:0:0:0:0",
				"a:b:c:d::",
				"a:b:c:d:0:0:0:0",
				"000a:000b:000c:000d:0000:0000:0000:0000",
				"a:b:c:d::",
				"a:b:c:d::",
				"a:b:c:d::",
				"a:b:c:d::",
				"a:b:c:d::0.0.0.0",
				"a:b:c:d::",
				"a:b:c:d::",
				"a:b:c:d::",
				"0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-0-0-0-0.ipv6-literal.net",
				"00|N0s0$ND2BxK96%Chk",
				"0x000a000b000c000d0000000000000000",
				"00000240001300006000032000000000000000000000");
		testIPv6Strings("a:b:c:d::/64",
				"a:b:c:d:0:0:0:0/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000:0000:0000:0000/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-0-0-0-0.ipv6-literal.net/64",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k/64",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("a::d:*:*:*:*/65",
				"a:0:0:d:*:0:0:0/65",
				"a:0:0:d:*:*:*:*",
				"a::d:*:*:*:*",
				"a:0:0:d:%:%:%:%",
				"000a:0000:0000:000d:0000-8000:0000:0000:0000/65",
				"a:0:0:d:*::/65",
				"a:0:0:d:*::/65",
				"a:0:0:d:*::/65",
				"a::d:*:*:*:*",
				"a::d:*:0:0.0.0.0/65",
				"a::d:*:0:0.0.0.0/65",
				"a:0:0:d:*::/65",
				"a:0:0:d:*::/65",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-d-*-0-0-0.ipv6-literal.net/65",
				"00|M>t|tt+WbKhfd5~qN�00|M>t|tt-R6^kVV>{?N/65",
				"0x000a00000000000d0000000000000000-0x000a00000000000dffffffffffffffff",
				"00000240000000000000032000000000000000000000-00000240000000000000033777777777777777777777");
		testIPv6Strings("a::d:*:0:0:0/65",
				"a:0:0:d:*:0:0:0/65",
				"a:0:0:d:*:*:*:*",
				"a::d:*:*:*:*",
				"a:0:0:d:%:%:%:%",
				"000a:0000:0000:000d:0000-8000:0000:0000:0000/65",
				"a:0:0:d:*::/65",
				"a:0:0:d:*::/65",
				"a:0:0:d:*::/65",
				"a::d:*:*:*:*",
				"a::d:*:0:0.0.0.0/65",
				"a::d:*:0:0.0.0.0/65",
				"a:0:0:d:*::/65",
				"a:0:0:d:*::/65",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-d-*-0-0-0.ipv6-literal.net/65",
				"00|M>t|tt+WbKhfd5~qN�00|M>t|tt-R6^kVV>{?N/65",
				"0x000a00000000000d0000000000000000-0x000a00000000000dffffffffffffffff",
				"00000240000000000000032000000000000000000000-00000240000000000000033777777777777777777777");
		testIPv6Strings("a:b:c:*::/64",
				"a:b:c:*:0:0:0:0/64",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*:*",
				"a:b:c:%:%:%:%:%",
				"000a:000b:000c:0000-ffff:0000:0000:0000:0000/64",
				"a:b:c:*::/64",
				"a:b:c:*::/64",
				"a:b:c:*::/64",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*::0.0.0.0/64",
				"a:b:c:*::0.0.0.0/64",
				"a:b:c:*::/64",
				"a:b:c:*::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-*-0-0-0-0.ipv6-literal.net/64",
				"00|N0s0$N0-%*(tF5l-X�00|N0s0;%a&*sUa#KSGX/64",
				"0x000a000b000c00000000000000000000-0x000a000b000cffffffffffffffffffff",
				"00000240001300006000000000000000000000000000-00000240001300006377777777777777777777777777");
		testIPv6Strings("a:b:c:d:*::/64",
				"a:b:c:d:0:0:0:0/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000:0000:0000:0000/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-0-0-0-0.ipv6-literal.net/64",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k/64",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("a::/64",
				"a:0:0:0:0:0:0:0/64",
				"a:0:0:0:*:*:*:*",
				"a::*:*:*:*",
				"a:0:0:0:%:%:%:%",
				"000a:0000:0000:0000:0000:0000:0000:0000/64",
				"a::/64",
				"a::/64",
				"a::/64",
				"a::*:*:*:*",
				"a::0.0.0.0/64",
				"a::0.0.0.0/64",
				"a::/64",
				"a::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.0.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-0-0-0-0-0.ipv6-literal.net/64",
				"00|M>t|ttwH6V62lVY`A�00|M>t|ttxBz48@eGWJA/64",
				"0x000a0000000000000000000000000000-0x000a000000000000ffffffffffffffff",
				"00000240000000000000000000000000000000000000-00000240000000000000001777777777777777777777");
		testIPv6Strings("a:b:c:*:*:*:*:*",//as noted above, addresses are not converted to prefix if starting as wildcards.  call toCIDREquivalent() or getMinPrefix()
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*:*",
				"a:b:c:%:%:%:%:%",
				"000a:000b:000c:0000-ffff:0000-ffff:0000-ffff:0000-ffff:0000-ffff",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*:*",
				"a:b:c:*:*:*:*.*.*.*",
				"a:b:c:*:*:*:*.*.*.*",
				"a:b:c:*:*:*:*.*.*.*",
				"a:b:c:*:*:*:*.*.*.*",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-*-*-*-*-*.ipv6-literal.net",
				"00|N0s0$N0-%*(tF5l-X�00|N0s0;%a&*sUa#KSGX",
				"0x000a000b000c00000000000000000000-0x000a000b000cffffffffffffffffffff",
				"00000240001300006000000000000000000000000000-00000240001300006377777777777777777777777777");
		testIPv6Strings("a:0:0:d:e:f:0:0/112",
				"a:0:0:d:e:f:0:0/112",
				"a:0:0:d:e:f:0:*",
				"a::d:e:f:0:*",
				"a:0:0:d:e:f:0:%",
				"000a:0000:0000:000d:000e:000f:0000:0000/112",
				"a::d:e:f:0:0/112",
				"a::d:e:f:0:0/112",
				"a:0:0:d:e:f::/112",
				"a::d:e:f:0:*",
				"a::d:e:f:0.0.0.0/112",
				"a::d:e:f:0.0.0.0/112",
				"a::d:e:f:0.0.0.0/112",
				"a:0:0:d:e:f::/112",
				"*.*.*.*.0.0.0.0.f.0.0.0.e.0.0.0.d.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-d-e-f-0-0.ipv6-literal.net/112",
				"00|M>t|tt+WcwbECb*xq/112",
				"0x000a00000000000d000e000f00000000-0x000a00000000000d000e000f0000ffff",
				"00000240000000000000032000160000740000000000-00000240000000000000032000160000740000177777");
		testIPv6Strings("a:0:c:d:e:f:0:0/112",
				"a:0:c:d:e:f:0:0/112",			
				"a:0:c:d:e:f:0:*",
				"a:0:c:d:e:f:0:*",
				"a:0:c:d:e:f:0:%",
				"000a:0000:000c:000d:000e:000f:0000:0000/112",
				"a:0:c:d:e:f::/112",
				"a:0:c:d:e:f::/112",
				"a:0:c:d:e:f::/112",
				"a::c:d:e:f:0:*",
				"a::c:d:e:f:0.0.0.0/112",
				"a::c:d:e:f:0.0.0.0/112",
				"a::c:d:e:f:0.0.0.0/112",
				"a:0:c:d:e:f::/112",
				"*.*.*.*.0.0.0.0.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-e-f-0-0.ipv6-literal.net/112",
				"00|M>t};s?v~hFl`j3_$/112",
				"0x000a0000000c000d000e000f00000000-0x000a0000000c000d000e000f0000ffff",
				"00000240000000006000032000160000740000000000-00000240000000006000032000160000740000177777");
		testIPv6Strings("a:0:c:d:e:f:0:0/97",
				"a:0:c:d:e:f:0:0/97",		
				"a:0:c:d:e:f:0-7fff:*",
				"a:0:c:d:e:f:0-7fff:*",
				"a:0:c:d:e:f:0-7fff:%",
				"000a:0000:000c:000d:000e:000f:0000:0000/97",
				"a:0:c:d:e:f::/97",
				"a:0:c:d:e:f::/97",
				"a:0:c:d:e:f::/97",
				"a::c:d:e:f:0-7fff:*",
				"a::c:d:e:f:0.0.0.0/97",
				"a::c:d:e:f:0.0.0.0/97",
				"a::c:d:e:f:0.0.0.0/97",
				"a:0:c:d:e:f::/97",
				"*.*.*.*.*.*.*.0-7.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-e-f-0-0.ipv6-literal.net/97",
				"00|M>t};s?v~hFl`j3_$/97",
				"0x000a0000000c000d000e000f00000000-0x000a0000000c000d000e000f7fffffff",
				"00000240000000006000032000160000740000000000-00000240000000006000032000160000757777777777");
		testIPv6Strings("a:0:c:d:e:f:0:0/96",
				"a:0:c:d:e:f:0:0/96",			
				"a:0:c:d:e:f:*:*",
				"a:0:c:d:e:f:*:*",
				"a:0:c:d:e:f:%:%",
				"000a:0000:000c:000d:000e:000f:0000:0000/96",
				"a:0:c:d:e:f::/96",
				"a:0:c:d:e:f::/96",
				"a:0:c:d:e:f::/96",
				"a::c:d:e:f:*:*",
				"a::c:d:e:f:0.0.0.0/96",
				"a::c:d:e:f:0.0.0.0/96",
				"a:0:c:d:e:f::/96",
				"a:0:c:d:e:f::/96",
				"*.*.*.*.*.*.*.*.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-e-f-0-0.ipv6-literal.net/96",
				"00|M>t};s?v~hFl`j3_$/96",
				"0x000a0000000c000d000e000f00000000-0x000a0000000c000d000e000fffffffff",
				"00000240000000006000032000160000740000000000-00000240000000006000032000160000777777777777");
		testIPv6Strings("a:0:c:d:e:f:1:0/112",
				"a:0:c:d:e:f:1:0/112",
				"a:0:c:d:e:f:1:*",
				"a:0:c:d:e:f:1:*",
				"a:0:c:d:e:f:1:%",
				"000a:0000:000c:000d:000e:000f:0001:0000/112",
				"a::c:d:e:f:1:0/112",//compressed
				"a:0:c:d:e:f:1:0/112",//canonical (only zeros are single so not compressed)
				"a:0:c:d:e:f:1::/112",//subnet
				"a::c:d:e:f:1:*",//compressed wildcard
				"a::c:d:e:f:0.1.0.0/112",//mixed, no compress
				"a::c:d:e:f:0.1.0.0/112",//mixed, no compress host
				"a::c:d:e:f:0.1.0.0/112",
				"a::c:d:e:f:0.1.0.0/112",
				"*.*.*.*.1.0.0.0.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-e-f-1-0.ipv6-literal.net/112",
				"00|M>t};s?v~hFl`jD0%/112",
				"0x000a0000000c000d000e000f00010000-0x000a0000000c000d000e000f0001ffff",
				"00000240000000006000032000160000740000200000-00000240000000006000032000160000740000377777");//mixed
		testIPv6Strings("a:0:c:d:0:0:1:0/112",
				"a:0:c:d:0:0:1:0/112", //normalized
				"a:0:c:d:0:0:1:*",//normalized wildcard
				"a:0:c:d::1:*",//canonical wildcard
				"a:0:c:d:0:0:1:%",//sql
				"000a:0000:000c:000d:0000:0000:0001:0000/112", //full
				"a:0:c:d::1:0/112",//compressed
				"a:0:c:d::1:0/112",//canonical 
				"a:0:c:d:0:0:1::/112",//subnet
				"a:0:c:d::1:*",//compressed wildcard
				"a:0:c:d::0.1.0.0/112",//mixed, no compress
				"a:0:c:d::0.1.0.0/112",//mixed, no compress host
				"a:0:c:d::0.1.0.0/112",
				"a:0:c:d::0.1.0.0/112",
				"*.*.*.*.1.0.0.0.0.0.0.0.0.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-0-0-1-0.ipv6-literal.net/112",
				"00|M>t};s?v}5L>MDR^a/112",
				"0x000a0000000c000d0000000000010000-0x000a0000000c000d000000000001ffff",
				"00000240000000006000032000000000000000200000-00000240000000006000032000000000000000377777");//mixed
		testIPv6Strings("a:0:c:d:e:f:a:0/112",
				"a:0:c:d:e:f:a:0/112",
				"a:0:c:d:e:f:a:*",
				"a:0:c:d:e:f:a:*",
				"a:0:c:d:e:f:a:%",
				"000a:0000:000c:000d:000e:000f:000a:0000/112",
				"a::c:d:e:f:a:0/112",
				"a:0:c:d:e:f:a:0/112",
				"a:0:c:d:e:f:a::/112",
				"a::c:d:e:f:a:*",
				"a::c:d:e:f:0.10.0.0/112",
				"a::c:d:e:f:0.10.0.0/112",
				"a::c:d:e:f:0.10.0.0/112",
				"a::c:d:e:f:0.10.0.0/112",
				"*.*.*.*.a.0.0.0.f.0.0.0.e.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-e-f-a-0.ipv6-literal.net/112",
				"00|M>t};s?v~hFl`k9s=/112",
				"0x000a0000000c000d000e000f000a0000-0x000a0000000c000d000e000f000affff",
				"00000240000000006000032000160000740002400000-00000240000000006000032000160000740002577777");
		testIPv6Strings("a:0:c:d:0:0:0:100/120",
				"a:0:c:d:0:0:0:100/120", //normalized
				"a:0:c:d:0:0:0:100-1ff",//normalized wildcard
				"a:0:c:d::100-1ff",//canonical wildcard
				"a:0:c:d:0:0:0:1__",//sql
				"000a:0000:000c:000d:0000:0000:0000:0100/120", //full
				"a:0:c:d::100/120",//compressed
				"a:0:c:d::100/120",//canonical 
				"a:0:c:d::100/120",//subnet
				"a:0:c:d::100-1ff",//compressed wildcard
				"a:0:c:d::0.0.1.0/120",//mixed, no compress
				"a:0:c:d::0.0.1.0/120",//mixed, no compress host
				"a:0:c:d::0.0.1.0/120",
				"a:0:c:d::0.0.1.0/120",
				"*.*.1.0.0.0.0.0.0.0.0.0.0.0.0.0.d.0.0.0.c.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-c-d-0-0-0-100.ipv6-literal.net/120",
				"00|M>t};s?v}5L>MDI>a/120",
				"0x000a0000000c000d0000000000000100-0x000a0000000c000d00000000000001ff",
				"00000240000000006000032000000000000000000400-00000240000000006000032000000000000000000777");//mixed
		testIPv6Strings("a:b:c:d:*", 
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000-ffff:0000-ffff:0000-ffff:0000-ffff",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*.*.*.*",
				"a:b:c:d:*:*:*.*.*.*",
				"a:b:c:d:*:*:*.*.*.*",
				"a:b:c:d:*:*:*.*.*.*",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-*-*-*-*.ipv6-literal.net",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000-ffff:0000-ffff:0000-ffff:0000-ffff",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*.*.*.*",
				"a:b:c:d:*:*:*.*.*.*",
				"a:b:c:d:*:*:*.*.*.*",
				"a:b:c:d:*:*:*.*.*.*",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-*-*-*-*.ipv6-literal.net",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("a:b:c:d:*/64",
				"a:b:c:d:0:0:0:0/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000:0000:0000:0000/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-0-0-0-0.ipv6-literal.net/64",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k/64",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("a:b:c:d:*:*:*:*/64",
				"a:b:c:d:0:0:0:0/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d:%:%:%:%",
				"000a:000b:000c:000d:0000:0000:0000:0000/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"a:b:c:d:*:*:*:*",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::0.0.0.0/64",
				"a:b:c:d::/64",
				"a:b:c:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.c.0.0.0.b.0.0.0.a.0.0.0.ip6.arpa",
				"a-b-c-d-0-0-0-0.ipv6-literal.net/64",
				"00|N0s0$ND2BxK96%Chk�00|N0s0$ND{&WM}~o9(k/64",
				"0x000a000b000c000d0000000000000000-0x000a000b000c000dffffffffffffffff",
				"00000240001300006000032000000000000000000000-00000240001300006000033777777777777777777777");
		testIPv6Strings("a::c:d:*",
				"a:0:0:0:0:c:d:*",
				"a:0:0:0:0:c:d:*",
				"a::c:d:*",
				"a:0:0:0:0:c:d:%",
				"000a:0000:0000:0000:0000:000c:000d:0000-ffff",
				"a::c:d:*",
				"a::c:d:*",
				"a::c:d:*",
				"a::c:d:*",
				"a::c:0.13.*.*",
				"a::c:0.13.*.*",
				"a::c:0.13.*.*",
				"a::c:0.13.*.*",
				"*.*.*.*.d.0.0.0.c.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-0-0-c-d-*.ipv6-literal.net",
				"00|M>t|ttwH6V6EEzblZ�00|M>t|ttwH6V6EEzkrZ",
				"0x000a0000000000000000000c000d0000-0x000a0000000000000000000c000dffff",
				"00000240000000000000000000000000600003200000-00000240000000000000000000000000600003377777");
		testIPv6Strings("a::d:*:*:*:*",
				"a:0:0:d:*:*:*:*",
				"a:0:0:d:*:*:*:*",
				"a::d:*:*:*:*",
				"a:0:0:d:%:%:%:%",
				"000a:0000:0000:000d:0000-ffff:0000-ffff:0000-ffff:0000-ffff",
				"a::d:*:*:*:*",
				"a::d:*:*:*:*",
				"a::d:*:*:*:*",
				"a::d:*:*:*:*",
				"a::d:*:*:*.*.*.*",
				"a::d:*:*:*.*.*.*",
				"a::d:*:*:*.*.*.*",
				"a::d:*:*:*.*.*.*",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-d-*-*-*-*.ipv6-literal.net",
				"00|M>t|tt+WbKhfd5~qN�00|M>t|tt-R6^kVV>{?N",
				"0x000a00000000000d0000000000000000-0x000a00000000000dffffffffffffffff",
				"00000240000000000000032000000000000000000000-00000240000000000000033777777777777777777777");
		testIPv6Strings("a::c:d:*/64",
				"a:0:0:0:0:0:0:0/64",
				"a:0:0:0:*:*:*:*",
				"a::*:*:*:*",
				"a:0:0:0:%:%:%:%",
				"000a:0000:0000:0000:0000:0000:0000:0000/64",
				"a::/64",
				"a::/64",
				"a::/64",
				"a::*:*:*:*",
				"a::0.0.0.0/64",
				"a::0.0.0.0/64",
				"a::/64",
				"a::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.0.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-0-0-0-0-0.ipv6-literal.net/64",
				"00|M>t|ttwH6V62lVY`A�00|M>t|ttxBz48@eGWJA/64",
				"0x000a0000000000000000000000000000-0x000a000000000000ffffffffffffffff",
				"00000240000000000000000000000000000000000000-00000240000000000000001777777777777777777777");
		testIPv6Strings("a::d:*:*:*:*/64",
				"a:0:0:d:0:0:0:0/64",
				"a:0:0:d:*:*:*:*",
				"a::d:*:*:*:*",
				"a:0:0:d:%:%:%:%",
				"000a:0000:0000:000d:0000:0000:0000:0000/64",
				"a:0:0:d::/64",
				"a:0:0:d::/64",
				"a:0:0:d::/64",
				"a::d:*:*:*:*",
				"a::d:0:0:0.0.0.0/64",
				"a::d:0:0:0.0.0.0/64",
				"a:0:0:d::/64",
				"a:0:0:d::/64",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.d.0.0.0.0.0.0.0.0.0.0.0.a.0.0.0.ip6.arpa",
				"a-0-0-d-0-0-0-0.ipv6-literal.net/64",
				"00|M>t|tt+WbKhfd5~qN�00|M>t|tt-R6^kVV>{?N/64",
				"0x000a00000000000d0000000000000000-0x000a00000000000dffffffffffffffff",
				"00000240000000000000032000000000000000000000-00000240000000000000033777777777777777777777");
		testIPv6Strings("1::/32",
				"1:0:0:0:0:0:0:0/32",
				"1:0:*:*:*:*:*:*",
				"1:0:*:*:*:*:*:*",
				"1:0:%:%:%:%:%:%",
				"0001:0000:0000:0000:0000:0000:0000:0000/32",
				"1::/32",
				"1::/32",
				"1::/32",
				"1::*:*:*:*:*:*",
				"1::0.0.0.0/32",
				"1::0.0.0.0/32",
				"1::/32",
				"1::/32",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.0.0.0.0.1.0.0.0.ip6.arpa",
				"1-0-0-0-0-0-0-0.ipv6-literal.net/32",
				"008JOm8Mm5*yBppL!sg1�008JPeGE6kXzV|T&xr^1/32",
				"0x00010000000000000000000000000000-0x00010000ffffffffffffffffffffffff",
				"00000020000000000000000000000000000000000000-00000020000077777777777777777777777777777777");
		testIPv6Strings("ffff::/8",
				"ff00:0:0:0:0:0:0:0/8",
				"ff00-ffff:*:*:*:*:*:*:*",
				"ff00-ffff:*:*:*:*:*:*:*",
				"ff__:%:%:%:%:%:%:%",
				"ff00:0000:0000:0000:0000:0000:0000:0000/8",
				"ff00::/8",
				"ff00::/8",
				"ff00::/8",
				"ff00-ffff:*:*:*:*:*:*:*",
				"ff00::0.0.0.0/8",
				"ff00::0.0.0.0/8",
				"ff00::/8",
				"ff00::/8",
				"*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.*.f.f.ip6.arpa",
				"ff00-0-0-0-0-0-0-0.ipv6-literal.net/8",
				"=SN{mv>Qn+T=L9X}Vo30�=r54lj&NUUO~Hi%c2ym0/8",
				"0xff000000000000000000000000000000-0xffffffffffffffffffffffffffffffff",
				"03770000000000000000000000000000000000000000-03777777777777777777777777777777777777777777");
		testIPv6Strings("ffff::/104",
				"ffff:0:0:0:0:0:0:0/104",
				"ffff:0:0:0:0:0:0-ff:*",
				"ffff::0-ff:*",
				"ffff:0:0:0:0:0:0-ff:%",
				"ffff:0000:0000:0000:0000:0000:0000:0000/104",
				"ffff::/104",
				"ffff::/104",
				"ffff::/104",
				"ffff::0-ff:*",
				"ffff::0.0.0.0/104",
				"ffff::0.0.0.0/104",
				"ffff::0.0.0.0/104",
				"ffff::/104",
				"*.*.*.*.*.*.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.f.f.f.f.ip6.arpa",
				"ffff-0-0-0-0-0-0-0.ipv6-literal.net/104",
				"=q{+M|w0(OeO5^EGP660/104",
				"0xffff0000000000000000000000000000-0xffff0000000000000000000000ffffff",
				"03777760000000000000000000000000000000000000-03777760000000000000000000000000000077777777");
		testIPv6Strings("ffff::/108",
				"ffff:0:0:0:0:0:0:0/108",
				"ffff:0:0:0:0:0:0-f:*",
				"ffff::0-f:*",
				"ffff:0:0:0:0:0:_:%",
				"ffff:0000:0000:0000:0000:0000:0000:0000/108",
				"ffff::/108",
				"ffff::/108",
				"ffff::/108",
				"ffff::0-f:*",
				"ffff::0.0.0.0/108",
				"ffff::0.0.0.0/108",
				"ffff::0.0.0.0/108",
				"ffff::/108",
				"*.*.*.*.*.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.f.f.f.f.ip6.arpa",
				"ffff-0-0-0-0-0-0-0.ipv6-literal.net/108",
				"=q{+M|w0(OeO5^EGP660/108",
				"0xffff0000000000000000000000000000-0xffff00000000000000000000000fffff",
				"03777760000000000000000000000000000000000000-03777760000000000000000000000000000003777777");
		testIPv6Strings("ffff::1000:0/108",
				"ffff:0:0:0:0:0:1000:0/108",
				"ffff:0:0:0:0:0:1000-100f:*",
				"ffff::1000-100f:*",
				"ffff:0:0:0:0:0:100_:%",
				"ffff:0000:0000:0000:0000:0000:1000:0000/108",
				"ffff::1000:0/108",
				"ffff::1000:0/108",
				"ffff:0:0:0:0:0:1000::/108",
				"ffff::1000-100f:*",
				"ffff::16.0.0.0/108",
				"ffff::16.0.0.0/108",
				"ffff::16.0.0.0/108",
				"ffff::16.0.0.0/108",
				"*.*.*.*.*.0.0.1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.f.f.f.f.ip6.arpa",
				"ffff-0-0-0-0-0-1000-0.ipv6-literal.net/108",
				"=q{+M|w0(OeO5^ELbE%G/108",
				"0xffff0000000000000000000010000000-0xffff00000000000000000000100fffff",
				"03777760000000000000000000000000002000000000-03777760000000000000000000000000002003777777");
		testIPv6Strings("ffff::a000:0/108",
				"ffff:0:0:0:0:0:a000:0/108",
				"ffff:0:0:0:0:0:a000-a00f:*",
				"ffff::a000-a00f:*",
				"ffff:0:0:0:0:0:a00_:%",
				"ffff:0000:0000:0000:0000:0000:a000:0000/108",
				"ffff::a000:0/108",
				"ffff::a000:0/108",
				"ffff:0:0:0:0:0:a000::/108",
				"ffff::a000-a00f:*",
				"ffff::160.0.0.0/108",
				"ffff::160.0.0.0/108",
				"ffff::160.0.0.0/108",
				"ffff::160.0.0.0/108",
				"*.*.*.*.*.0.0.a.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.f.f.f.f.ip6.arpa",
				"ffff-0-0-0-0-0-a000-0.ipv6-literal.net/108",
				"=q{+M|w0(OeO5^E(z82>/108",
				"0xffff00000000000000000000a0000000-0xffff00000000000000000000a00fffff",
				"03777760000000000000000000000000024000000000-03777760000000000000000000000000024003777777");
		testIPv6Strings("ffff::eeee:eeee/108",
				"ffff:0:0:0:0:0:eee0:0/108",
				"ffff:0:0:0:0:0:eee0-eeef:*",
				"ffff::eee0-eeef:*",
				"ffff:0:0:0:0:0:eee_:%",
				"ffff:0000:0000:0000:0000:0000:eee0:0000/108",
				"ffff::eee0:0/108",
				"ffff::eee0:0/108",
				"ffff:0:0:0:0:0:eee0::/108",
				"ffff::eee0-eeef:*",
				"ffff::238.224.0.0/108",
				"ffff::238.224.0.0/108",
				"ffff::238.224.0.0/108",
				"ffff::238.224.0.0/108",
				"*.*.*.*.*.e.e.e.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.f.f.f.f.ip6.arpa",
				"ffff-0-0-0-0-0-eee0-0.ipv6-literal.net/108",
				"=q{+M|w0(OeO5^F85=Cb/108",
				"0xffff00000000000000000000eee00000-0xffff00000000000000000000eeefffff",
				"03777760000000000000000000000000035670000000-03777760000000000000000000000000035673777777");
		testIPv6Strings("ffff::/107",
				"ffff:0:0:0:0:0:0:0/107",
				"ffff:0:0:0:0:0:0-1f:*",
				"ffff::0-1f:*",
				"ffff:0:0:0:0:0:0-1f:%",
				"ffff:0000:0000:0000:0000:0000:0000:0000/107",
				"ffff::/107",
				"ffff::/107",
				"ffff::/107",
				"ffff::0-1f:*",
				"ffff::0.0.0.0/107",
				"ffff::0.0.0.0/107",
				"ffff::0.0.0.0/107",
				"ffff::/107",
				"*.*.*.*.*.0-1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.f.f.f.f.ip6.arpa",
				"ffff-0-0-0-0-0-0-0.ipv6-literal.net/107",
				"=q{+M|w0(OeO5^EGP660/107",
				"0xffff0000000000000000000000000000-0xffff00000000000000000000001fffff",
				"03777760000000000000000000000000000000000000-03777760000000000000000000000000000007777777");
		testIPv6Strings("abcd::/107",
				"abcd:0:0:0:0:0:0:0/107",
				"abcd:0:0:0:0:0:0-1f:*",
				"abcd::0-1f:*",
				"abcd:0:0:0:0:0:0-1f:%",
				"abcd:0000:0000:0000:0000:0000:0000:0000/107",
				"abcd::/107",
				"abcd::/107",
				"abcd::/107",
				"abcd::0-1f:*",
				"abcd::0.0.0.0/107",
				"abcd::0.0.0.0/107",
				"abcd::0.0.0.0/107",
				"abcd::/107",
				"*.*.*.*.*.0-1.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.d.c.b.a.ip6.arpa",
				"abcd-0-0-0-0-0-0-0.ipv6-literal.net/107",
				"o6)n`s#^$cP5&p^H}p=a/107",
				"0xabcd0000000000000000000000000000-0xabcd00000000000000000000001fffff",
				"02536320000000000000000000000000000000000000-02536320000000000000000000000000000007777777");
		testIPv6Strings("1:2:3:4::%:%:%", //Note: % is the zone character (not sql wildcard), so this is handled as 1:2:3:4:: with zone :%:%
				"1:2:3:4:0:0:0:0%:%:%", //normalized
				"1:2:3:4:0:0:0:0%:%:%", //normalizedWildcards
				"1:2:3:4::%:%:%", //canonicalWildcards
				"1:2:3:4:0:0:0:0%:%:%", //sql
				"0001:0002:0003:0004:0000:0000:0000:0000%:%:%",
				"1:2:3:4::%:%:%",//compressed
				"1:2:3:4::%:%:%",//canonical
				"1:2:3:4::%:%:%",//subnet
				"1:2:3:4::%:%:%",//compressed wildcard
				"1:2:3:4::0.0.0.0%:%:%",//mixed no compress
				"1:2:3:4::%:%:%",//mixedNoCompressHost
				"1:2:3:4::%:%:%",
				"1:2:3:4::%:%:%",
				"0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.4.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.ip6.arpa",
				"1-2-3-4-0-0-0-0s-s-s.ipv6-literal.net",
				"008JQWOV7Skb)C|ve)jA�:%:%",
				"0x00010002000300040000000000000000%:%:%",
				"00000020000200001400010000000000000000000000%:%:%");//mixed
		testIPv6Strings("1:2:3:4::*:*:*",
				"1:2:3:4:0:*:*:*", //normalized
				"1:2:3:4:0:*:*:*", //normalizedWildcards
				"1:2:3:4:0:*:*:*", //canonicalWildcards
				"1:2:3:4:0:%:%:%", //sql
				"0001:0002:0003:0004:0000:0000-ffff:0000-ffff:0000-ffff",
				"1:2:3:4::*:*:*",//compressed
				"1:2:3:4:0:*:*:*",//canonical
				"1:2:3:4::*:*:*",//subnet
				"1:2:3:4::*:*:*",//compressed wildcard
				"1:2:3:4::*:*.*.*.*",//mixed no compress
				"1:2:3:4::*:*.*.*.*",//mixedNoCompressHost
				"1:2:3:4::*:*.*.*.*",
				"1:2:3:4::*:*.*.*.*",
				"*.*.*.*.*.*.*.*.*.*.*.*.0.0.0.0.4.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.ip6.arpa",
				"1-2-3-4-0-*-*-*.ipv6-literal.net",
				"008JQWOV7Skb)C|ve)jA�008JQWOV7Skb?_P3;X#A",
				"0x00010002000300040000000000000000-0x00010002000300040000ffffffffffff",
				"00000020000200001400010000000000000000000000-00000020000200001400010000007777777777777777");
		testIPv6Strings("1:2:3:4::/80",
				"1:2:3:4:0:0:0:0/80", //normalized
				"1:2:3:4:0:*:*:*", //normalizedWildcards
				"1:2:3:4:0:*:*:*", //canonicalWildcards
				"1:2:3:4:0:%:%:%", //sql
				"0001:0002:0003:0004:0000:0000:0000:0000/80",
				"1:2:3:4::/80",//compressed
				"1:2:3:4::/80",
				"1:2:3:4::/80",
				"1:2:3:4::*:*:*",
				"1:2:3:4::0.0.0.0/80",//mixed no compress
				"1:2:3:4::0.0.0.0/80",//mixedNoCompressHost
				"1:2:3:4::/80",
				"1:2:3:4::/80",
				"*.*.*.*.*.*.*.*.*.*.*.*.0.0.0.0.4.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.ip6.arpa",
				"1-2-3-4-0-0-0-0.ipv6-literal.net/80",
				"008JQWOV7Skb)C|ve)jA/80",
				"0x00010002000300040000000000000000-0x00010002000300040000ffffffffffff",
				"00000020000200001400010000000000000000000000-00000020000200001400010000007777777777777777");
		testIPv6Strings("1:2:3:4::",
				"1:2:3:4:0:0:0:0", //normalized
				"1:2:3:4:0:0:0:0", //normalizedWildcards
				"1:2:3:4::", //canonicalWildcards
				"1:2:3:4:0:0:0:0", //sql
				"0001:0002:0003:0004:0000:0000:0000:0000",
				"1:2:3:4::",//compressed
				"1:2:3:4::",
				"1:2:3:4::",
				"1:2:3:4::",
				"1:2:3:4::0.0.0.0",//mixed no compress
				"1:2:3:4::",//mixedNoCompressHost
				"1:2:3:4::",
				"1:2:3:4::",
				"0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.0.4.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.ip6.arpa",
				"1-2-3-4-0-0-0-0.ipv6-literal.net",
				"008JQWOV7Skb)C|ve)jA",
				"0x00010002000300040000000000000000",
				"00000020000200001400010000000000000000000000");//mixed 
		testIPv6Strings("1:2:3:4:0:6::",
				"1:2:3:4:0:6:0:0", //normalized
				"1:2:3:4:0:6:0:0", //normalizedWildcards
				"1:2:3:4:0:6::", //canonicalWildcards
				"1:2:3:4:0:6:0:0", //sql
				"0001:0002:0003:0004:0000:0006:0000:0000",
				"1:2:3:4:0:6::",//compressed
				"1:2:3:4:0:6::",
				"1:2:3:4:0:6::",//subnet
				"1:2:3:4:0:6::",//compressedWildcard
				"1:2:3:4::6:0.0.0.0",//mixed no compress
				"1:2:3:4:0:6::",//mixedNoCompressHost
				"1:2:3:4:0:6::",
				"1:2:3:4:0:6::",
				"0.0.0.0.0.0.0.0.6.0.0.0.0.0.0.0.4.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.ip6.arpa",
				"1-2-3-4-0-6-0-0.ipv6-literal.net",
				"008JQWOV7Skb)D3fCrWG",
				"0x00010002000300040000000600000000",
				"00000020000200001400010000000000300000000000");
		testIPv6Strings("1:2:3:0:0:6::",
				"1:2:3:0:0:6:0:0", //normalized
				"1:2:3:0:0:6:0:0", //normalizedWildcards
				"1:2:3::6:0:0", //canonicalWildcards
				"1:2:3:0:0:6:0:0", //sql
				"0001:0002:0003:0000:0000:0006:0000:0000",
				"1:2:3::6:0:0",//compressed
				"1:2:3::6:0:0",
				"1:2:3::6:0:0",//subnet
				"1:2:3::6:0:0",//compressedWildcard
				"1:2:3::6:0.0.0.0",//mixed no compress
				"1:2:3::6:0.0.0.0",//mixedNoCompressHost
				"1:2:3::6:0.0.0.0",
				"1:2:3:0:0:6::",
				"0.0.0.0.0.0.0.0.6.0.0.0.0.0.0.0.0.0.0.0.3.0.0.0.2.0.0.0.1.0.0.0.ip6.arpa",
				"1-2-3-0-0-6-0-0.ipv6-literal.net",
				"008JQWOV7O(=61h*;$LC",
				"0x00010002000300000000000600000000",
				"00000020000200001400000000000000300000000000");
	}
	
	void testPrefix(String original, Integer prefixLength, int minPrefix, Integer equivalentPrefix) {
		IPAddress mac = createAddress(original).getAddress();
		testPrefix(mac, prefixLength, minPrefix, equivalentPrefix);
		incrementTestCount();
	}
	
	@Override
	boolean allowsRange() {
		return true;
	}

	
	
	
	@Override
	void runTest() {
		testEquivalentPrefix("*.*.*.*", 0);
		testEquivalentPrefix("0-127.*.*.*", 1);
		testEquivalentPrefix("128-255.*.*.*", 1);
		testEquivalentPrefix("*.*.*.*/1", 0);
		testEquivalentPrefix("0.*.*.*/1", 1);
		testEquivalentPrefix("1.2.*.*", 16);
		testEquivalentPrefix("1.2.*.0/24", 16);
		testEquivalentPrefix("1.2.0-255.0/24", 16);
		testEquivalentPrefix("1.2.1.0/24", 24);
		testEquivalentPrefix("1.2.*.4", null, 32);
		testEquivalentPrefix("1.2.252-255.*", 22);
		testEquivalentPrefix("1.2.252-255.0-255", 22);
		testEquivalentPrefix("1.2.0-3.0-255", 22);
		testEquivalentPrefix("1.2.128-131.0-255", 22);
		testEquivalentPrefix("1.2.253-255.0-255", null, 24);
		testEquivalentPrefix("1.2.252-255.0-254", null, 32);
		testEquivalentPrefix("1.2.251-255.0-254", null, 32);
		testEquivalentPrefix("1.2.251-255.0-255", null, 24);
		
		testEquivalentPrefix("*:*", 0);
		testEquivalentPrefix("::/0", 0);
		testEquivalentPrefix("::/1", 1);
		testEquivalentPrefix("1:2:*", 32);
		testEquivalentPrefix("1:2:*:*::/64", 32);
		testEquivalentPrefix("1:2:*::/64", null, 64);
		testEquivalentPrefix("1:2:*::", null, 128);
		testEquivalentPrefix("1:2:8000-ffff:*", 33);
		testEquivalentPrefix("1:2:0000-7fff:*", 33);
		testEquivalentPrefix("1:2:c000-ffff:*", 34);
		testEquivalentPrefix("1:2:0000-3fff:*", 34);
		testEquivalentPrefix("1:2:8000-bfff:*", 34);
		testEquivalentPrefix("1:2:4000-7fff:*", 34);
		testEquivalentPrefix("1:2:fffc-ffff:*", 46);
		testEquivalentPrefix("1:2:fffc-ffff:0-ffff:*", 46);
		testEquivalentPrefix("1:2:fffd-ffff:0-ffff:*", null, 48);
		testEquivalentPrefix("1:2:fffc-ffff:0-fffe:*", null, 64);
		testEquivalentPrefix("1:2:fffb-ffff:0-fffe:*", null, 64);
		testEquivalentPrefix("1:2:fffb-ffff:0-ffff:*", null, 48);
		
		testReverseHostAddress("*.*.*.0/20");
		testReverseHostAddress("*.*.0.0/16");
		testReverseHostAddress("*:*::/20");
		
		testStrings();
		
		testTrees();
		
		testReverse("1:2:*:4:5:6:a:b", false, false);
		testReverse("1:1:1:1-fffe:2:3:3:3", false, false);
		testReverse("1:1:1:0-fffe:1-fffe:*:1:1", false, false);
		testReverse("ffff:80:*:ff:01:ffff", false, false);
		testReverse("ffff:8000:fffe::7fff:0001:ffff", true, false);
		testReverse("ffff:8000:*:8000:1:*:01:ffff", true, false);
		testReverse("ffff:8118:ffff:*:1-fffe:ffff", false, true);
		testReverse("ffff:8181:c3c3::4224:2400:0-fffe", false, true);
		testReverse("ffff:1:ff:ff:*:*", false, false);
		
		
		testMatches(true, "1.2.3.4/16", "1.2.*.*");
		testMatches(true, "1.2.3.4/16", "1.2.*");
		testMatches(false, "1.2.3.4/15", "1.2.*.*");
		testMatches(false, "1.2.3.4/17", "1.2.*.*");
		
		testMatches(true, "1.2.3.4/16", "1.2.*/255.255.0.0");
		testMatches(true, "1.2.3.4/15", "1.2.3.*/255.254.0.0");
		testMatches(true, "1.2.3.4/17", "1.2.3.*/255.255.128.0");
		
		testMatches(false, "1.1.3.4/15", "1.2.3.*/255.254.0.0");
		testMatches(false, "1.1.3.4/17", "1.2.3.*/255.255.128.0");
		
		testMatches(true, "1:2::/32", "1:2:*:*:*:*:*:*");
		testMatches(true, "1:2::/32", "1:2:*:*:*:*:*.*.*.*");
		testMatches(true, "1:2::/32", "1:2:*");
		testMatches(false, "1:2::/32", "1:2:*:*:*:*:3:*");
		testMatches(false, "1:2::/32", "1:2:*:*:*:*:*.*.3.*");
		testMatches(false, "1:2::/31", "1:2:*");
		testMatches(false, "1:2::/33", "1:2::*");
		
		testMatches(true, "1:2::/32", "1:2:*:*:*:*:*:*/ffff:ffff::");
		testMatches(true, "1:2::/31", "1:2:*:*:*:*:*:*/ffff:fffe::");
		testMatches(true, "1:2::/33", "1:2:0:*:*:*:*:*/ffff:ffff:8000::");
		
		testMatches(true, "1:2::/24", "1:__:*");
		testMatches(true, "1:2::/28", "1:_::/32");
		testMatches(true, "1:2::/20", "1:___::/32");
		testMatches(true, "1:2::/16", "1:____::/32");
		testMatches(true, "1:ffef::/24", "1:ff__::/32");
		testMatches(true, "1:ffef::/24", "1:ff__:*:*");
		testMatches(true, "250-255.200-255.0-255.20-29", "25_.2__.___.2_");
		testMatches(true, "150-159.100-199.0-99.10-19", "15_.1__.__.1_");
		testMatches(false, "251-255.200-255.0-255.20-29", "25_.2__.___.2_");
		testMatches(false, "150-158.100-199.0-99.10-19", "15_.1__.__.1_");
		testMatches(true, "250-25f:200-2ff:0-fff:20-2f::", "25_:2__:___:2_::");
		testMatches(true, "150-15f:100-1ff:0-ff:10-1f::", "15_:1__:__:1_::");
		testMatches(false, "250-25f:201-2ff:0-fff:20-2f::", "25_:2__:___:2_::");
		testMatches(false, "150-15f:100-1ef:0-ff:10-1f::", "15_:1__:__:1_::");
		testMatches(true, "::250-25f:200-2ff:0-fff:20-2f", "::25_:2__:___:2_");
		testMatches(true, "::150-15f:100-1ff:0-ff:10-1f", "::15_:1__:__:1_");
		testMatches(true, "250-25f:200-2ff::0-fff:20-2f", "25_:2__::___:2_");
		testMatches(true, "150-15f:100-1ff::0-ff:10-1f", "15_:1__::__:1_");
		
		testMatches(true, "1:2:3:4:5:6:1.2.0.4-5", "1:2:3:4:5:6:102:4-5"); //mixed ending with range
		testMatches(true, "1:2:3:4:5:6:1.2.0.*", "1:2:3:4:5:6:102:0-ff"); //mixed ending with range
		testMatches(true, "1:2:3:4:5:6:1.2.0._", "1:2:3:4:5:6:102:0-9"); //mixed ending with range
		testMatches(true, "1:2:3:4:5:6:1.2.0.1_", "1:2:3:4:5:6:102:a-13"); //mixed ending with range
		
		testMatches(true, "1.2.3", "1.2.0.3", true);
		testMatches(true, "1.2.2-3.4", "0x1.0x2.2-0x3.0x4", true);
		testMatches(true, "1.2.2-3.4", "0x1.0x2.0x2-0x3.0x4", true);
		testMatches(true, "1.2.2-3.4", "0x1.0x2.0x2-3.0x4", true);
		testMatches(true, "1.2.2-3.4", "01.02.2-03.04", true);
		testMatches(true, "1.2.2-3.4", "01.02.2-3.04", true);
		testMatches(true, "1.2.2-3.4", "01.02.02-03.04", true);
		testMatches(true, "1.2.2-3.4", "01.02.0x2-03.04", true);
		testMatches(true, "1.2.2-3.4", "01.02.0x2-0x3.04", true);
		testMatches(true, "1.2.0200-0277.4", "01.02.02__.04", true);
		testMatches(true, "1.2.0x20-0x2f.4", "01.02.0x2_.04", true);
		testMatches(true, "1.2.0x10-0x1f.4", "01.02.0x1_.04", true);
		testMatches(true, "1.2.*.4", "01.02.0x__.04", true);
		testMatches(true, "1.2.0-077.4", "01.02.0__.04", true);
		
		testMatches(true, "1.2.2-3.4", "01.02.0x2-0x3.04", true);
		
		testMatches(true, "0.0.0-1.4", "00.0x0.0x00-0x000001.04", true);
		testMatches(true, "11.10-11.10-11.10-11", "11.012-0xb.0xa-013.012-0xB", true);
		testMatches(true, "11.10-11.*.10-11", "11.012-0xb.0x0-0xff.012-0xB", true);
		testMatches(true, "1.*", "1.*.0x0-0xff", true);
		testMatches(true, "1.*", "1.0-255.0-65535", true);
		testMatches(true, "1.*", "1.0-0xff.0-0xffff", true);
		testMatches(true, "1.*", "1.0x0-0xff.00-0xffff", true);
		
		testMatches(true, "11.11.0-11.*", "11.11.0-0xbff", true);
		testMatches(true, "11.0.0.11-11", "11.0x00000000000000000b-0000000000000000000013", true);
		testMatches(true, "11.1-11.*/16", "11.0x10000-786431/16", true);
		testMatches(true, "11.1-11.*/16", "11.0x10000-0xbffff/16", true);
		
		testMatches(true, "1:2:3:4:5:6:1.2.3.4/96", "1:2:3:4:5:6:*:*");
		testMatches(true, "1:2:3:4:5:6:255.2.3.4/97", "1:2:3:4:5:6:8000-ffff:*");
		testMatches(true, "1:2:3:4:5:6:1.2.3.4/112", "1:2:3:4:5:6:102:*");
		testMatches(true, "1:2:3:4:5:6:1.2.255.4/115", "1:2:3:4:5:6:102:e000-ffff");
		testMatches(true, "1:2:3:4:5:6:1.2.3.4/128", "1:2:3:4:5:6:102:304");
		
		testMatches(true, "1.2.3.4/0", "*.*");
		testMatches(true, "1.2.3.4/0", "*.*.*.*");
		testMatches(true, "1:2:3:4:5:6:7:8/0", "*:*");
		testMatches(true, "1:2:3:4:5:6:7:8/0", "*:*:*:*:*:*:*:*");
		
		testMatches(true, "1-02.03-4.05-06.07", "1-2.3-4.5-6.7");
		testMatches(true, "1-002.003-4.005-006.007", "1-2.3-4.5-6.7");
		
		testMatches(true, "1-2.0-0.00-00.00-0", "1-2.0.0.0");
		testMatches(true, "1-2:0-0:00-00:00-0:0-000:0000-0000:0000-00:0000-0", "1-2:0:0:0:0:0:0:0");
		testMatches(true, "00-0.0-0.00-00.00-0", "0.0.0.0");
		testMatches(true, "0-00:0-0:00-00:00-0:0-000:0000-0000:0000-00:0000-0", "::");
		
		testMatches(true, "-1.22.33.4", "0-1.22.33.4");
		testMatches(true, "-1.22.33.4", "0-1.22.33.4");
		testMatches(true, "22.1-.33.4", "22.1-255.33.4");
		testMatches(true, "22.33.4.1-", "22.33.4.1-255");
		testMatches(true, "aa:-1:cc::d:ee:f", "aa:0-1:cc::d:ee:f");
		testMatches(true, "aa:dd-:cc::d:ee:f", "aa:dd-ffff:cc::d:ee:f");
		testMatches(true, "::1:0:0:0.0.0.0", "0:0:0:1::0.0.0.0");
		
		testMasks("9.*.237.26/0", "0.0.0.0/0");
		testMasks("9.*.237.26/1", "0.0.0.0/1");
		testMasks("9.*.237.26/4", "0.0.0.0/4");
		testMasks("9.*.237.26/5", "8.0.0.0/5");
		testMasks("9.*.237.26/7", "8.0.0.0/7");
		testMasks("9.*.237.26/8", "9.0.0.0/8");
		testMasks("9.*.237.26/9", "9.*.0.0/9");
		testMasks("9.*.237.26/16", "9.*.0.0/16");
		testMasks("9.*.237.26/30", "9.*.237.24/30");//the mask makes these two the same
		testMasks("9.*.237.26/32", "9.*.237.26/32");
		
		testSubnet("1.2-4.3.4", "255.255.254.255", 24, "1.2-4.2.0/24", "1.2-4.2.4", "1.2-4.3.0/24");
		testSubnet("1.2-4.3.4", "255.248.254.255", 24, "1.0.2.0/24", "1.0.2.4", "1.2-4.3.0/24");
		
		testSubnet("__::", "ffff::", 128, "0-ff:0:0:0:0:0:0:0/128", "0-ff:0:0:0:0:0:0:0", "0-ff:0:0:0:0:0:0:0/128");
		testSubnet("0-ff::", "fff0::", 128, null, null, "0-ff:0:0:0:0:0:0:0/128");
		testSubnet("0-ff::", "fff0::", 12, IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0-f0:0:0:0:0:0:0:0/12" : "0-ff:0:0:0:0:0:0:0/12", null, IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0-f0:0:0:0:0:0:0:0/12" : "0-ff:0:0:0:0:0:0:0/12");
		testSubnet("0-f::*", "fff0::ffff", 12, "0:0:0:0:0:0:0:0/12", "0:0:0:0:0:0:0:*", "0:0:0:0:0:0:0:0/12");
		testSubnet("::1:__", "::1:ffff", 128, "0:0:0:0:0:0:1:0-ff/128", "0:0:0:0:0:0:1:0-ff", "0:0:0:0:0:0:1:0-ff/128");
		testSubnet("::1:__", "::1:ffff", 126, IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0:0:0:0:0:0:1:0-fc/126" : "0:0:0:0:0:0:1:0-ff/126", "0:0:0:0:0:0:1:0-ff", IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0:0:0:0:0:0:1:0-fc/126" : "0:0:0:0:0:0:1:0-ff/126");
		testSubnet("::1:0-ff", "::1:fff0", 128, null, null, "0:0:0:0:0:0:1:0-ff/128");
		testSubnet("::1:0-ff", "::1:fff0", 124, IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0:0:0:0:0:0:1:0-f0/124" : "0:0:0:0:0:0:1:0-ff/124", null, IPAddressSegment.ADJUST_RANGES_BY_PREFIX ? "0:0:0:0:0:0:1:0-f0/124" : "0:0:0:0:0:0:1:0-ff/124");
		testSubnet("*::1:0-f", "ffff::1:fff0", 124, "*:0:0:0:0:0:1:0/124", "*:0:0:0:0:0:1:0", "*:0:0:0:0:0:1:0/124");
		
		testBitwiseOr("1.2.0.0/16", 8, "0.0.3.248", "1.2.3.248-255");
		testBitwiseOr("1.2.0.0/16", 7, "0.0.2.0", "1.2.2-3.*");
		testBitwiseOr("1.2.0.0/16", 7, "0.0.3.0", "1.2.3.*");
		testBitwiseOr("0.0.0.0/0", 0, "128.192.224.240", "128-255.192-255.224-255.240-255");
		testBitwiseOr("0.0.0.0/0", 0, "128.192.224.64", null);
		
		testPrefixBitwiseOr("0.0.0.0/16", 18, "0.0.2.8", "0.0.*.0/18");
		
		
		testBitwiseOr("1:2::/32", 16, "0:0:3:fff8::", "1:2:3:fff8-ffff:*");
		testBitwiseOr("1:2::/32", 15, "0:0:2::", "1:2:2-3:*");
		testBitwiseOr("1:2::/32", 15, "0:0:3::", "1:2:3:*");
		testBitwiseOr("::/0", 0, "8000:c000:e000:fff0::", "8000-ffff:c000-ffff:e000-ffff:fff0-ffff:*");
		testBitwiseOr("::/0", 0, "8000:c000:e000:4000::", null);
		
		testPrefixBitwiseOr("::/32", 34, "0:0:2:8::", "0:0:*::/34");
		
		testDelimitedCount("1,2-3,4:3:4,5:6:7:8:ffff:ffff", 8); 
		testDelimitedCount("1,2::3,6:7:8:4,5-6:6,8", 16);
		testDelimitedCount("1:2:3:*:4::5", 1);
		testDelimitedCount("1:2,3,*:3:ffff:ffff:6:4:5,ff,7,8,99", 15);
		testDelimitedCount("0,1-2,3,5:3::6:4:5,ffff,7,8,99", 30);
		
		testContains("0.0.0.0/0", "1-2.*.3.*", false);
		testContains("0-127.0.0.0/1", "127-127.*.3.*", false);
		testContains("0.0.0.0/4", "13-15.*.3.*", false);
		testContains("1-2.0.0.0/4", "9.*.237.*/16", false);
		testContains("1-2.0.0.0/4", "8-9.*.237.*/16", false);
		testNotContains("1-2.0.0.0/4", "9-17.*.237.*/16");
		testContains("8.0.0.0/5", "15.2.3.4", false);
		testContains("8.0.0.0/7", "8-9.*.3.*", false);
		testContains("9.0.0.0/8", "9.*.3.*", false);
		testContains("9.128.0.0/9", "9.128-255.*.0", false);
		testContains("9.128.0.0/15", "9.128-129.3.*", false);
		testContains("9.129.0.0/16", "9.129.3.*", false);
		testNotContains("9.129.0.0/16", "9.128-129.3.*");
		testNotContains("9.129.0.0/16", "9.128.3.*");
		testContains("9.129.237.24/30", "9.129.237.24-27", true);
		testContains("9.129.237.24/30", "9.129.237.24-27/31", true);

		testContains("9.129.237.26/0", "*.*.*.*/0", true);
		testContains("0-127.129.237.26/1", "0-127.0.*.0/1", true);
		testContains("9.129.237.26/4", "0-15.0.0.*/4", true);
		testNotContains("9.129.237.26/4", "16-17.0.0.*/4");
		testContains("1-16.0.0.*/4", "9.129.237.26/4", false);
		testContains("9.129.237.26/5", "8-15.0.0.0/5", true);
		testContains("9.129.237.26/7", "8-9.0.0.1-3/7", true);
		testNotContains("9.129.237.26/7", "2.0.0.1-3/7");
		testContains("7-9.0.0.1-3/7", "9.129.237.26/7", false);
		testContains("9.129.237.26/8", "9.*.0.0/8", true);
		testContains("9.129.237.26/9", "9.128-255.0.0/9", true);
		testContains("9.129.237.26/15", "9.128-129.0.*/15", true);
		testContains("9.129.237.26/16", "9.129.*.*/16", true);
		testContains("9.129.237.26/30", "9.129.237.24-27/30", true);
		testContains("9.128-129.*.26/32", "9.128-129.*.26/32", true);

		testContains("::ffff:1.*.3.4", "1.2.3.4", false);//ipv4 mapped
		testContains("::ffff:1.2-4.3.4/112", "1.2-3.3.*", false);

		testContains("0:0:0:0:0:0:0:0/0", "a:*:c:d:e:1-ffff:a:b", false);
		testContains("8000:0:0:0:0:0:0:0/1", "8000-8fff:b:c:d:e:f:*:b", false);
		testNotContains("8000:0:0:0:0:0:0:0/1", "7fff-8fff:b:c:d:e:f:*:b");
		testContains("ffff:0:0:0:0:0:0:0/30", "ffff:0-3:c:d:e:f:a:b", false);
		testNotContains("ffff:0:0:0:0:0:0:0/30", "ffff:0-4:c:d:e:f:a:b");
		testContains("ffff:0:0:0:0:0:*:0/32", "ffff:0:ffff:1-d:e:f:*:b", false);
		testNotContains("ffff:0:0:0:0:1-2:0:0/32", "ffff:0-1:ffff:d:e:f:a:b");
		testContains("ffff:0:0:0:0:4-ffff:0:fffc/126", "ffff:0:0:0:0:4-ffff:0:fffd-ffff", false);
		testContains("ffff:0:0:0:0:4-ffff:0:fffc/126", "ffff:0:0:0:0:4-ffff:0:fffc-ffff", true);
		testContains("ffff:0:*:0:0:4-ffff:0:ffff/128", "ffff:0:*:0:0:4-ffff:0:ffff", true);
		
		testContains("ffff::ffff/0", "a-b:0:b:0:c:d-e:*:0/0", true);
		testContains("ffff::ffff/1", "8000-8fff:0:0:0:0:*:a-b:0/1", true);
		testContains("fffc-ffff::ffff/30", "fffd-fffe:0:0:0:0:0:0:0/30", false);
		testContains("ffff:0-d::ffff/32", "ffff:a-c:0:0:0:0:0:0/32", false);
		testContains("ffff:*:0:0:0:0:0:fffa-ffff/126", "ffff:*::ffff/126", false);
		testContains("ffff:*::ffff/126", "ffff:*:0:0:0:0:0:fffc-ffff/126", true);
		testContains("ffff:1-2::ffff/126", "ffff:1-2:0:0:0:0:0:fffc-ffff/126", true);
		
		testPrefix("25:51:27:*:*:*:*:*", null, 48, 48);
		testPrefix("25:51:27:*:*:*:*:*/48", 48, 48, 48);
		testPrefix("25:50-51:27::/48", 48, 48, null);
		testPrefix("25:50-51:27:*:*:*:*:*", null, 48, null);
		testPrefix("25:51:27:12:82:55:2:2", null, 128, 128);
		testPrefix("*:*:*:*:*:*:*:*", null, 0, 0);
		testPrefix("*:*:*:*:*:*:0-fe:*", null, 112, null);
		testPrefix("*:*:*:*:*:*:0-ff:*", null, 104, null);
		testPrefix("*:*:*:*:*:*:0-ffff:*", null, 0, 0);
		testPrefix("*:*:*:*:*:*:0-7fff:*", null, 97, null);
		testPrefix("*:*:*:*:*:*:8000-ffff:*", null, 97, null);
		testPrefix("*.*.*.*", null, 0, 0);
		testPrefix("3.*.*.*", null, 8, 8);
		testPrefix("3.*.*.1-3", null, 32, null);
		testPrefix("3.0-127.*.*", null, 9, 9);
		testPrefix("3.128-255.*.*", null, 9, 9);
		
		
		ipv4test(true, "1.2.*.4/1");
		ipv4test(false, "1.2.*.4/-1");
		ipv4test(false, "1.2.*.4/");
		ipv4test(false, "1.2.*.4/x");
		ipv4test(false, "1.2.*.4/33");//we are not allowing extra-large prefixes
		ipv6test(true, "1:*::1/1");
		ipv6test(false, "1:*::1/-1");
		ipv6test(false, "1:*::1/");
		ipv6test(false, "1:*::1/x");
		ipv6test(false, "1:*::1/129");//we are not allowing extra-large prefixes
		
		//masks that have wildcards in them
		ipv4test(false, "1.2.3.4/*");
		ipv4test(false, "1.2.*.4/*");
		ipv4test(false, "1.2.3.4/1-2.2.3.4");
		ipv4test(false, "1.2.*.4/1-2.2.3.4");
		ipv4test(false, "1.2.3.4/**");
		ipv4test(false, "1.2.*.4/**");
		ipv4test(false, "1.2.3.4/*.*");
		ipv4test(false, "1.2.*.4/*.*");
		ipv4test(false, "1.2.3.4/*:*");
		ipv4test(false, "1.2.*.4/*:*");
		ipv4test(false, "1.2.3.4/*:*:*:*:*:*:*:*");
		ipv4test(false, "1.2.*.4/*:*:*:*:*:*:*:*");
		ipv4test(false, "1.2.3.4/1.2.*.4");
		ipv4test(false, "1.2.*.4/1.2.*.4");
		ipv4test(true, "1.2.*.4/1.2.3.4");
		ipv6test(false, "1:2::1/*");
		ipv6test(false, "1:*::1/*");
		ipv6test(false, "1:2::1/1:1-2:3:4:5:6:7:8");
		ipv6test(false, "1:*::1/1:1-2:3:4:5:6:7:8");
		ipv6test(false, "1:2::1/**");
		ipv6test(false, "1:*::1/**");
		ipv6test(false, "1:2::1/*:*");
		ipv6test(false, "1:*::1/*:*");
		ipv6test(false, "1:2::1/*.*");
		ipv6test(false, "1:*::1/*.*");
		ipv6test(false, "1:2::1/*.*.*.*");
		ipv6test(false, "1:*::1/*.*.*.*");
		ipv6test(false, "1:2::1/1:*::2");
		ipv6test(false, "1:*::1/1:*::2");
		ipv6test(true, "1:*::1/1::2");
		
		testResolved("8.*.27.26", "8.*.27.26");
		
		testResolved("2001:*:0:0:8:800:200C:417A", "2001:*:0:0:8:800:200C:417A");
		
		testNormalized("ABCD:EF12:*:*:***:A:*:BBBB", "abcd:ef12:*:*:*:a:*:bbbb");
		testNormalized("ABCD:EF12:*:*:**:A:***:BBBB%g", "abcd:ef12:*:*:*:a:*:bbbb%g");
		
		testNormalized("1.*", "1.*.*.*");
		testNormalized("*.1.*", "*.1.*.*");
		testNormalized("*:1::*", "*:1::*");
		testNormalized("*:1:*", "*:1:*:*:*:*:*:*");
		testNormalized("001-002:0001-0002:01-2:1-02:01-02:*", "1-2:1-2:1-2:1-2:1-2:*:*:*");
		
		testIPv4Wildcarded("1.2.3.4", 8, "1.*.*.*", "1.%.%.%");
		testIPv4Wildcarded("1.2.3.4", 9, "1.0-127.*.*", "1.0-127.%.%");
		testIPv4Wildcarded("1.2.3.4", 15, "1.2-3.*.*", "1.2-3.%.%");
		testIPv4Wildcarded("1.3.3.4", 15, "1.2-3.*.*", "1.2-3.%.%");
		testIPv4Wildcarded("1.2.3.4", 16, "1.2.*.*", "1.2.%.%");
		testWildcarded("1:0::", 32, "1::/32", "1:0:*:*:*:*:*:*", "1:0:*:*:*:*:*:*", "1::*:*:*:*:*:*", "1:0:%:%:%:%:%:%");
		testIPv6Wildcarded("1::", 16, "1::/16", "1:*:*:*:*:*:*:*", "1:%:%:%:%:%:%:%");
		testIPv6Wildcarded("1::", 20, "1::/20", "1:0-fff:*:*:*:*:*:*", "1:0-fff:%:%:%:%:%:%");
		testIPv6Wildcarded("1:f000::", 20, "1:f000::/20", "1:f000-ffff:*:*:*:*:*:*", "1:f___:%:%:%:%:%:%");
		testIPv6Wildcarded("1::", 17, "1::/17", "1:0-7fff:*:*:*:*:*:*", "1:0-7fff:%:%:%:%:%:%");
		testIPv6Wildcarded("1:10::", 28, "1:10::/28", "1:10-1f:*:*:*:*:*:*", "1:1_:%:%:%:%:%:%");
		testIPv6Wildcarded("1::", 28, "1::/28", "1:0-f:*:*:*:*:*:*", "1:_:%:%:%:%:%:%");
		testIPv6Wildcarded("1::", 31, "1::/31", "1:0-1:*:*:*:*:*:*", "1:0-1:%:%:%:%:%:%");
		testWildcarded("1::", 36, "1::/36", "1:0:0-fff:*:*:*:*:*", "1:0:0-fff:*:*:*:*:*", "1::0-fff:*:*:*:*:*", "1:0:0-fff:%:%:%:%:%");
		testWildcarded("1::", 52, "1::/52", "1:0:0:0-fff:*:*:*:*", "1::0-fff:*:*:*:*", "1::0-fff:*:*:*:*", "1:0:0:0-fff:%:%:%:%");
		testWildcarded("1::", 60, "1::/60", "1:0:0:0-f:*:*:*:*", "1::0-f:*:*:*:*", "1::0-f:*:*:*:*", "1:0:0:_:%:%:%:%");
		
		testCount("1.2.3.4", 1);
		testCount("1.2.3.4/32", 1);
		testCount("1.2.3.4/31", 2);
		testCount("1.2.3.4/30", 4);
		testCount("1.1-2.3.4", 2, RangeParameters.WILDCARD_AND_RANGE);
		testCount("1.*.3.4", 256);
		
		//these can take a while, since they generate 48640, 65536, and 32758 addresses respectively
		testCount("1.*.11-200.4", 190 * 256, RangeParameters.WILDCARD_AND_RANGE);
		testCount("1.3.*.4/16", 256 * 256);
		testCount("1.2.*.1-3/25", 256 * 128, RangeParameters.WILDCARD_AND_RANGE);
		
		ipv4test(true, "1.1.*.100-101", RangeParameters.WILDCARD_AND_RANGE);
		ipv4test(false, "1.2.*.101-100", RangeParameters.WILDCARD_AND_RANGE);//downwards range
		ipv4test(true, "1.2.*.101-101", RangeParameters.WILDCARD_AND_RANGE);
		ipv6test(true, "1:2:4:a-ff:0-2::1", RangeParameters.WILDCARD_AND_RANGE);
		ipv6test(false, "1:2:4:ff-a:0-2::1", RangeParameters.WILDCARD_AND_RANGE);//downwards range
		ipv4test(false, "1.2.*.101-100/24", RangeParameters.WILDCARD_AND_RANGE);//downwards range but ignored due to CIDR
		
		//these tests create strings that validate ipv4 and ipv6 differently, allowing ranges for one and not the other
		ipv4test(true, "1.*.3.4", RangeParameters.WILDCARD_AND_RANGE, RangeParameters.NO_RANGE);
		ipv4test(false, "1.*.3.4", RangeParameters.NO_RANGE, RangeParameters.WILDCARD_AND_RANGE);
		ipv6test(false, "a:*::1.*.3.4", RangeParameters.WILDCARD_AND_RANGE, RangeParameters.NO_RANGE);
		ipv6test(true, "a:*::1.*.3.4", RangeParameters.NO_RANGE, RangeParameters.WILDCARD_AND_RANGE);
		ipv6test(false, "a:*::", RangeParameters.WILDCARD_AND_RANGE, RangeParameters.NO_RANGE);
		ipv6test(true, "a:*::", RangeParameters.NO_RANGE, RangeParameters.WILDCARD_AND_RANGE);
		
		
//		octal, hex, dec overflow
//		do it with 1, 2, 3, 4 segments
		ipv4_inet_aton_test(true, "0.0.0.1-255");
		ipv4_inet_aton_test(false, "0.0.0.1-256");
		ipv4_inet_aton_test(true, "0.0.512-65535");
		ipv4_inet_aton_test(false, "0.0.512-65536");
		ipv4_inet_aton_test(true, "0.65536-16777215");
		ipv4_inet_aton_test(false, "0.65536-16777216");
		ipv4_inet_aton_test(true, "16777216-4294967295");
		ipv4_inet_aton_test(false, "16777216-4294967296");
		ipv4_inet_aton_test(false, "0.0.0.0x1x");
		ipv4_inet_aton_test(false, "0.0.0.1x");
		ipv4_inet_aton_test(true, "0.0.0.0x1-0xff");
		ipv4_inet_aton_test(false, "0.0.0.0x1-0x100");
		ipv4_inet_aton_test(true, "0.0.0xfffe-0xffff");
		ipv4_inet_aton_test(false, "0.0.0xfffe-0x10000");
		ipv4_inet_aton_test(false, "0.0.0x10000-0x10001");
		ipv4_inet_aton_test(true, "0.0-0xffffff");
		ipv4_inet_aton_test(false, "0.0-0x1000000");
		ipv4_inet_aton_test(true, "0x11000000-0xffffffff");
		ipv4_inet_aton_test(false, "0x11000000-0x100000000");
		ipv4_inet_aton_test(false, "0x100000000-0x100ffffff");
		ipv4_inet_aton_test(true, "0.0.0.00-0377");
		ipv4_inet_aton_test(false, "0.0.0.00-0400");
		ipv4_inet_aton_test(true, "0.0.0x100-017777");
		ipv4_inet_aton_test(false, "0.0.0x100-0200000");
		ipv4_inet_aton_test(true, "0.0x10000-077777777");
		//ipv4_inet_aton_test(false, "0.0x1-077777777"); the given address throw AddressTypeException as expected, would need to rewrite the test to make that a pass
		ipv4_inet_aton_test(false, "0.0x10000-0100000000");
		ipv4_inet_aton_test(true, "0x1000000-03777777777");
		ipv4_inet_aton_test(true, "0x1000000-037777777777");
		ipv4_inet_aton_test(false, "0x1000000-040000000000");
		
		ipv4test(true, "*"); //toAddress() should not work on this, toAddress(Version) should.
		
		ipv4test(false, "*%", false, true); //because the string could represent ipv6, and we are allowing zone, we treat the % as ipv6 zone, and then we invalidate because no zone for ipv4
		ipv4test(false, "*%x", false, true); //no zone for ipv4
		ipv4test(true, "**"); //toAddress() should not work on this, toAddress(Version) should.
		ipv6test(true, "*%x"); //ipv6 which allows zone
		
		ipv4test(true, "*.*.*.*"); //toAddress() should work on this 
		
		ipv4test(true, "1.*.3");
		
		ipv4test(false, "a.*.3.4");
		ipv4test(false, "*.a.3.4");
		ipv4test(false, "1.*.a.4");
		ipv4test(false, "1.*.3.a");
		
		ipv4test(false, ".2.3.*");
		ipv4test(false, "1..*.4");
		ipv4test(false, "1.*..4");
		ipv4test(false, "*.2.3.");
		
		ipv4test(false, "256.*.3.4");
		ipv4test(false, "1.256.*.4");
		ipv4test(false, "*.2.256.4");
		ipv4test(false, "1.*.3.256");
		
		
		ipv4test(true, "0.0.*.0", false);
		ipv4test(true, "00.*.0.0", false);
		ipv4test(true, "0.00.*.0", false);
		ipv4test(true, "0.*.00.0", false);
		ipv4test(true, "*.0.0.00", false);
		ipv4test(true, "000.0.*.0", false);
		ipv4test(true, "0.000.0.*", false);
		ipv4test(true, "*.0.000.0", false);
		ipv4test(true, "0.0.*.000", false);
		
		ipv4test(true, "0.0.*.0", false);
		ipv4test(true, "00.*.0.0", false);
		ipv4test(true, "0.00.*.0", false);
		ipv4test(true, "0.*.00.0", false);
		ipv4test(true, "*.0.0.00", false); 
		ipv4test(true, "000.0.*.0", false);
		ipv4test(true, "0.000.0.*", false);
		ipv4test(true, "*.0.000.0", false);
		ipv4test(true, "0.0.*.000", false);
		
		ipv4test(true, "000.000.000.*", false);
		
		ipv4test(isLenient(), "0000.0.*.0");
		ipv4test(isLenient(), "*.0000.0.0");
		ipv4test(isLenient(), "0.*.0000.0");
		ipv4test(isLenient(), "*.0.0.0000");
		
		ipv4test(false, ".0.*.0");
		ipv4test(false, "0..*.0");
		ipv4test(false, "0.*..0");
		ipv4test(false, "*.0.0.");
		
		ipv4test(true, "1.*.3.4/255.1.0.0");
		ipv4test(false, "1.*.3.4/255.1.0.0/16");
		ipv4test(false, "1.*.3.4/255.*.0.0");//range in mask
		ipv4test(false, "1.*.3.4/255.1-2.0.0");//range in mask
		ipv4test(false, "1.*.3.4/1::1");//mask mismatch
		ipv6test(false, "1:*::/1.2.3.4");//mask mismatch
		
		ipv4test(false, "1.2.3.4/255.*.0.0");//range in mask
		ipv4test(false, "1.2.3.4/255.1-2.0.0");//range in mask
		ipv6test(false, "1:2::/1:*::");//range in mask
		ipv6test(false, "1:2::/1:1-2::");//range in mask
		
		ipv4testOnly(false, "1:2:3:4:5:*:7:8"); //fixed
		ipv4testOnly(false, "*::1"); //fixed
		
		ipv6test(1, "*"); //toAddress() should not work on this, toAddress(version) should
		ipv6test(1, "*%"); //toAddress() should not work on this, toAddress(version) should
		
		ipv6test(1, "*:*:*:*:*:*:*:*"); //toAddress() should work on this
		
		ipv6test(1,"*::1");// loopback, compressed, non-routable
		
		//this one test can take a while, since it generates (0xffff + 1) = 65536 addresses
		if(fullTest) testCount("*::1", 0xffff + 1);
		
		testCount("1-3::1", 3, RangeParameters.WILDCARD_AND_RANGE);
		testCount("0-299::1", 0x299 + 1, RangeParameters.WILDCARD_AND_RANGE);
		
		//this one test can take a while, since it generates 3 * (0xffff + 1) = 196606 addresses
		if(fullTest) testCount("1:2:4:*:0-2::1", 3 * (0xffff + 1), RangeParameters.WILDCARD_AND_RANGE);
		
		testCount("1:2:4:0-2:0-2::1", 3 * 3, RangeParameters.WILDCARD_AND_RANGE);
		testCount("1::2:3", 1);
		testCount("1::2:3/128", 1);
		testCount("1::2:3/127", 2);
		
		ipv4test(true, "1.0-0.3.0");
		ipv4test(true, "1.0-3.3.0");
		ipv4test(true, "1.1-3.3.0");
		ipv6test(true, "1:0-0:2:0::");
		ipv6test(true, "1:0-3:2:0::");
		ipv6test(true, "1:1-3:2:0::");
		
		ipv6test(1,"::*", false);// unspecified, compressed, non-routable
		ipv6test(1,"0:0:*:0:0:0:0:1");// loopback, full
		ipv6test(1,"0:0:*:0:0:0:0:0", false);// unspecified, full
		ipv6test(1,"2001:*:0:0:8:800:200C:417A");// unicast, full
		ipv6test(1,"FF01:*:0:0:0:0:0:101");// multicast, full
		ipv6test(1,"2001:DB8::8:800:200C:*");// unicast, compressed
		ipv6test(1,"FF01::*:101");// multicast, compressed
		ipv6test(0,"2001:DB8:0:0:8:*:200C:417A:221");// unicast, full
		ipv6test(0,"FF01::101::*");// multicast, compressed
		ipv6test(1,"fe80::217:f2ff:*:ed62");
		
		
		
		ipv6test(1,"2001:*:1234:0000:0000:C1C0:ABCD:0876");
		ipv6test(1,"3ffe:0b00:0000:0000:0001:0000:*:000a");
		ipv6test(1,"FF02:0000:0000:0000:0000:0000:*:0001");
		ipv6test(1,"*:0000:0000:0000:0000:0000:0000:0001");
		ipv6test(0,"0000:0000:0000:0000:*0000:0000:0000:*0", true);
		ipv6test(isLenient(),"02001:*:1234:0000:0000:C1C0:ABCD:0876"); // extra 0 not allowed!
		ipv6test(0,"2001:0000:1234:0000:0*:C1C0:ABCD:0876"); // extra 0 not allowed!
		ipv6test(1,"2001:0000:1234:0000:*:C1C0:ABCD:0876"); 
		
		//ipv6test(1," 2001:0000:1234:0000:0000:C1C0:ABCD:0876"); // leading space
		//ipv6test(1,"2001:0000:1234:0000:0000:C1C0:ABCD:0876 "); // trailing space
		//ipv6test(1," 2001:0000:1234:0000:0000:C1C0:ABCD:0876  "); // leading and trailing space
		
		ipv6test(0,"2001:0000:1234:0000:0000:C1C0*:ABCD:0876  0"); // junk after valid address
		ipv6test(0,"0 2001:0000:123*:0000:0000:C1C0:ABCD:0876"); // junk before valid address
		ipv6test(0,"2001:0000:1234: 0000:0000:C1C0:*:0876"); // internal space
		
		
		
		ipv6test(1,"3ffe:0b00:*:0001:0000:0000:000a");
		ipv6test(0,"3ffe:0b00:1:0001:0000:0000:000a"); // seven segments
		ipv6test(0,"FF02:0000:0000:0000:0000:0000:0000:*:0001"); // nine segments
		ipv6test(0,"3ffe:*::1::a"); // double "::"
		ipv6test(0,"::1111:2222:3333:4444:5555:*::"); // double "::"
		ipv6test(1,"2::10");
		ipv6test(1,"ff02::1");
		ipv6test(1,"fe80:*::");
		ipv6test(1,"2002:*::");
		ipv6test(1,"2001:*::");
		ipv6test(1,"*:0db8:1234::");
		ipv6test(1,"::ffff:*:0");
		ipv6test(1,"*::1");
		ipv6test(1,"1:2:3:4:*:6:7:8");
		ipv6test(1,"1:2:*:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::*");
		ipv6test(1,"1:2:3:*::8");
		ipv6test(1,"1:2:3::8");
		ipv6test(1,"*:2::8");
		ipv6test(1,"1::*");
		ipv6test(1,"*::2:3:4:5:6:7");
		ipv6test(1,"*::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:*");
		ipv6test(1,"1::2:*:4");
		ipv6test(1,"1::*:3");
		ipv6test(1,"1::*");
		
		ipv6test(1,"::*:3:4:5:6:7:8");
		ipv6test(1,"*::2:3:4:5:6:7");
		ipv6test(1,"::*:3:4:5:6");
		ipv6test(1,"::*:3:4:5");
		ipv6test(1,"::2:3:*");
		ipv6test(1,"*::2:3");
		ipv6test(1,"::*");
		ipv6test(1,"1:*:3:4:5:6::");
		ipv6test(1,"1:2:3:4:*::");
		ipv6test(1,"1:2:3:*::");
		ipv6test(1,"1:2:3::*");
		ipv6test(1,"*:2::");
		ipv6test(1,"*::");
		ipv6test(1,"*:2:3:4:5::7:8");
		ipv6test(0,"1:2:3::4:5::7:*"); // Double "::"
		ipv6test(0,"12345::6:7:*");
		ipv6test(1,"1:2:3:4::*:*");
		ipv6test(1,"1:*:3::7:8");
		ipv6test(1,"*:*::7:8");
		ipv6test(1,"*::*:8");
			
		// Testing IPv4 addresses represented as dotted-quads
		// Leading zero's in IPv4 addresses not allowed: some systems treat the leading "0" in ".086" as the start of an octal number
		// Update: The BNF in RFC-3986 explicitly defines the dec-octet (for IPv4 addresses) not to have a leading zero
		//ipv6test(0,"fe80:0000:0000:*:0204:61ff:254.157.241.086");
		ipv6test(!isLenient(),"fe80:0000:0000:*:0204:61ff:254.157.241.086");
		//ipv6test(1,"::*:192.0.*.128");
		ipv6test(1,"::*:192.0.128.*"); 
		ipv6test(0,"XXXX:XXXX:XXXX:XXXX:XXXX:XXXX:1.2.3.4");
		//ipv6test(0,"1111:2222:*:4444:5555:6666:00.00.00.00");
		ipv6test(1,"1111:2222:*:4444:5555:6666:00.00.00.00");
		//ipv6test(0,"1111:2222:3333:4444:5555:6666:000.*.000.000");
		ipv6test(1,"1111:2222:3333:4444:5555:6666:000.*.000.000");
		ipv6test(0,"*:2222:3333:4444:5555:6666:256.256.256.256");
			
		ipv6test(1,"*:2222:3333:4444:5555:6666:123.123.123.123");
		ipv6test(1,"1111:*:3333:4444:5555::123.123.123.123");
		ipv6test(1,"1111:2222:*:4444::123.123.123.123");
		//ipv6test(1,"1111:2222:3333::*.123.123.123");//cannot be converted to ipv6 range
		ipv6test(1,"1111:2222:3333::*.*.123.123");
		//ipv6test(1,"1111:2222::123.123.*.123");//cannot be converted to ipv6 range
		ipv6test(1,"1111:2222::123.123.*.*");
		ipv6test(1,"1111:2222::123.123.123.*");
		ipv6test(1,"1111::123.*.123.123");
		ipv6test(1,"::123.123.123.*");
		ipv6test(1,"1111:2222:3333:4444::*:123.123.123.123");
		ipv6test(1,"1111:2222:*::6666:123.123.123.123");
		ipv6test(1,"*:2222::6666:123.123.123.123");
		//ipv6test(1,"1111::6666:*.123.123.*");//cannot be converted to ipv6 range
		ipv6test(1,"1111::6666:*.*.*.*");
		//ipv6test(1,"::6666:123.123.*.123");//cannot be converted to ipv6 range
		ipv6test(1,"::6666:123.123.2.123");
		ipv6test(1,"1111:*:3333::5555:6666:123.*.123.123");
		ipv6test(1,"1111:2222::*:6666:123.123.*.*");
		ipv6test(1,"1111::*:6666:*.*.123.123");
		ipv6test(1,"1111::*:6666:*.123.123");
		ipv6test(1,"::5555:6666:123.123.123.123");
		ipv6test(1,"1111:2222::4444:5555:*:123.123.123.123");
		ipv6test(1,"1111::4444:5555:6666:123.*.123.123");
		ipv6test(1,"*::4444:5555:6666:123.123.123.123");
		ipv6test(1,"1111::*:4444:5555:6666:123.123.123.123");
		ipv6test(1,"::2222:*:4444:5555:6666:123.123.123.123");
		ipv6test(1,"::*:*:*:*:*:*.*.*.*");
		ipv6test(1,"*::*:*:*:*:*.*.*.*");
		ipv6test(0,"*:::*:*:*:*.*.*.*");
		ipv6test(0,"*:::*:*:*:*:*.*.*.*");
		ipv6test(1,"*::*:*:*:*:*.*.*.*");
		ipv6test(0,"*::*:*:*:*:*:*.*.*.*");
		ipv6test(0,"*:*:*:*:*:*:*:*:*.*.*.*");
		ipv6test(0,"*:*:*:*:*:*:*::*.*.*.*");
		ipv6test(0,"*:*:*:*:*:*::*:*.*.*.*");
		ipv6test(1,"*:*:*:*:*:*:*.*.*.*");
		ipv6test(1,"*:*:*:*:*::*.*.*.*");
		ipv6test(1,"*:*:*:*::*:*.*.*.*");
		
		ipv6test(1,"::*", false);
		ipv6test(1,"*:0:0:0:0:0:0:*", false);

		// Additional cases: http://crisp.tweakblogs.net/blog/2031/ipv6-validation-%28and-caveats%29.html
		ipv6test(1,"0:a:b:*:d:e:f::");
		ipv6test(1,"::0:a:*:*:d:e:f"); // syntactically correct, but bad form (::0:... could be combined)
		ipv6test(1,"a:b:c:*:*:f:0::");
		ipv6test(0,"':10.*.0.1");
		
		
		ipv4test(true, "1.*.4");
		ipv4test(true, "1.2.*");
		ipv4test(true, "*.1");
		ipv4test(true, "1.*");
		ipv4test(true, "1.*.1");
		ipv4test(true, "1.*.*");
		ipv4test(true, "*.*.1");
		ipv4test(true, "*.1.*");
		ipv4test(isLenient(), "1");
		ipv4test(isLenient(), "1.1");
		ipv4test(isLenient(), "1.1.1");
		
		ipv4test(true, "*.1.2.*");
		ipv4test(true, "*.1.*.2");
		ipv4test(true, "*.*.*.2");
		ipv4test(true, "*.*.*.*");
		ipv4test(true, "1.*.2.*");
		ipv4test(true, "1.2.*.*");
		
		ipv4test(true, "*.*"); 
		ipv6test(true, "1::1.2.*");
		ipv6test(true, "1::1.2.**");
		ipv6test(false, "1::1.2.**z");
		ipv6test(true, "1::1.2.3.4");
		ipv6test(true, "1:*:1");
		ipv4test(true, "1.2.*");
		
		ipv4test(false, "%.%"); 
		ipv6test(false, "1::1.2.%");
		ipv6test(true, "1::1.2.*%");
		ipv6test(true, "1::1.2.*%z");
		ipv6test(false, "1:%:1");
		ipv6test(true, "1::%:1");
		ipv4test(true, "1.2.%"); //we allow this now, the % is seen as a wildcard because we are ipv4 - if we allow zone and the string can be interpreted as ipv6 then % is seen as zone character
		
		ipv6test(1, "1:*");
		ipv6test(1, "*:1:*");
		ipv6test(1, "*:1");
		
		//ipv6test(1, "*:1:1.*.1");//cannot be converted to ipv6 range
		ipv6test(1, "*:1:1.*.*");
		//ipv6test(1, "*:1:*.1");//cannot be converted to ipv6 range
		ipv6test(1, "*:1:*.1.1");
		ipv6test(1, "*:1:1.*");
		
		ipv6test(0, "1:1:1.*.1");
		ipv6test(0, "1:1:1.*.1.1");
		ipv6test(1, "1:1:*.*");
		ipv6test(1, "1:2:3:4:5:*.*");
		ipv6test(1, "1:2:3:4:5:6:*.*");
		ipv6test(0, "1:1:1.*");
		
		
		ipv6test(1, "1::1:1.*.*");
		ipv6test(1, "1::1:*.1.1");
		ipv6test(1, "1::1:1.*");
		
		ipv6test(1, "1:*.1.2");//in this one, the wildcard covers both ipv6 and ipv4 parts
		ipv6test(1, "1::*.1.2");//compression takes precedence so the wildcard does not cover both ipv6 and ipv4 parts
		ipv6test(1, "1::2:*.1.2");//compression takes precedence so the wildcard does not cover both ipv6 and ipv4 parts
		ipv6test(1, "::2:*.1.2");//compression takes precedence so the wildcard does not cover both ipv6 and ipv4 parts
		ipv6test(0, "1:1.*.2");
		ipv6test(0, "1:1.*.2.2");
		ipv6test(isLenient(), "1:*:1.2");
		
		
		ipv6test(1, "*:1:1.*");
		ipv6test(isLenient(), "*:1:1.2.3");
		ipv6test(1, "::1:1.*");
		ipv6test(isLenient(), "::1:1.2.3");
		
		ipv6test(1, "1:*:1");
		ipv6test(1, "1:*:1:1.1.*");
		ipv6test(1, "1:*:1:1.1.*.*");
		ipv6test(1, "1:*:1:*");
		ipv6test(1, "1:*:1:*.1.2");
		ipv6test(1, "1:*:1:1.*");
		ipv6test(isLenient(), "1:*:1:1.2.3");
		
		ipv6test(0, "1:*:1:2:3:4:5:6:7");
		ipv6test(0, "1:*:1:2:3:4:5:1.2.3.4");
		ipv6test(1, "1:*:2:3:4:5:1.2.3.4");
		ipv6test(0, "1:*:2:3:4:5:1.2.3.4.5");
		ipv6test(0, "1:1:2:3:4:5:1.2.3.4.5");
		ipv6test(0, "1:1:2:3:4:5:6:1.2.3.4");
		ipv6test(0, "1:1:2:3:4:5:6:1.*.3.4");
		ipv6test(1, "1:2:3:4:5:6:1.2.3.4");
		ipv6test(1, "1:2:3:4:5:6:1.*.3.4");
		
		
		ipv4test(true, "255._.3.4");
		ipv4test(true, "1.255._.4");
		ipv4test(true, "_.2.255.4");
		ipv4test(true, "1._.3.255");
		
		ipv4test(true, "255.__.3.4");
		ipv4test(true, "1.255.__.4");
		ipv4test(true, "__.2.255.4");
		ipv4test(true, "1.__.3.255");
		
		ipv4test(true, "255.___.3.4");
		ipv4test(true, "1.255.___.4");
		ipv4test(true, "___.2.255.4");
		ipv4test(true, "1.___.3.255");
		
		ipv4test(isLenient(), "255.____.3.4");
		ipv4test(isLenient(), "1.255.____.4");
		ipv4test(isLenient(), "____.2.255.4");
		ipv4test(isLenient(), "1.____.3.255");
		
		ipv4test(false, "255._2_.3.4");
		ipv4test(false, "1.255._2_.4");
		ipv4test(false, "_2_.2.255.4");
		ipv4test(false, "1._2_.3.255");
		
		ipv4test(true, "255.2__.3.4");
		ipv4test(true, "1.255.2__.4");
		ipv4test(true, "2__.2.255.4");
		ipv4test(true, "1.2__.3.255");
		
		ipv4test(true, "255.2_.3.4");
		ipv4test(true, "1.255.2_.4");
		ipv4test(true, "2_.2.255.4");
		ipv4test(true, "1.2_.3.255");
		
		ipv4test(false, "255.__2.3.4");
		ipv4test(false, "1.255.__2.4");
		ipv4test(false, "__2.2.255.4");
		ipv4test(false, "1.__2.3.255");
		
		ipv4test(true, "25_.__.3.4");
		ipv4test(true, "1.255.2__._");
		ipv4test(true, "2_.2_.255.__");
		ipv4test(false, "1.2__.3__.25_");
		ipv4test(true, "1.2__.3_.25_");
		ipv4test(true, "1.2__.2__.25_");
		
		ipv4test(false, "1.1--2.1.1");
		ipv4test(false, "1.1-2-3.1.1");
		ipv4test(false, "1.1-2-.1.1");
		ipv4test(false, "1.-1-2.1.1");
		
		ipv4test(false, "1.1_2_.1.1");
		ipv4test(false, "1.1_2.1.1");
		ipv4test(true, "1.1_.1.1");
		
		ipv6test(false, "1:1--2:1:1::");
		ipv6test(false, "1:1-2-3:1:1::");
		ipv6test(false, "1:1-2-:1:1::");
		ipv6test(false, "1:-1-2:1:1::");
		
		ipv6test(false, "1:1_2_:1.1::");
		ipv6test(false, "1:1_2:1:1::");
		ipv6test(true, "1:1_:1:1::");
		
		//double -
		// _4_ single char wildcards not in trailing position

		ipv6test(1,"::ffff:_:0");
		ipv6test(1,"_::1");
		ipv6test(1,"1:2:3:4:_:6:7:8");
		ipv6test(1,"1:2:_:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::_");
		ipv6test(1,"1:2:3:_::8");
		ipv6test(1,"_:2::8");
		ipv6test(1,"1::_");
		ipv6test(1,"_::2:3:4:5:6:7");
		ipv6test(1,"_::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:_");
		ipv6test(1,"1::2:_:4");
		ipv6test(1,"1::_:3");
		ipv6test(1,"1::_");
		
		ipv6test(1,"::ffff:__:0");
		ipv6test(1,"__::1");
		ipv6test(1,"1:2:3:4:__:6:7:8");
		ipv6test(1,"1:2:__:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::__");
		ipv6test(1,"1:2:3:__::8");
		ipv6test(1,"__:2::8");
		ipv6test(1,"1::__");
		ipv6test(1,"__::2:3:4:5:6:7");
		ipv6test(1,"__::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:__");
		ipv6test(1,"1::2:__:4");
		ipv6test(1,"1::__:3");
		ipv6test(1,"1::__");
		
		ipv6test(1,"::ffff:___:0");
		ipv6test(1,"___::1");
		ipv6test(1,"1:2:3:4:___:6:7:8");
		ipv6test(1,"1:2:___:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::___");
		ipv6test(1,"1:2:3:___::8");
		ipv6test(1,"___:2::8");
		ipv6test(1,"1::___");
		ipv6test(1,"___::2:3:4:5:6:7");
		ipv6test(1,"___::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:___");
		ipv6test(1,"1::2:___:4");
		ipv6test(1,"1::___:3");
		ipv6test(1,"1::___");
		
		ipv6test(1,"::ffff:____:0");
		ipv6test(1,"____::1");
		ipv6test(1,"1:2:3:4:____:6:7:8");
		ipv6test(1,"1:2:____:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::____");
		ipv6test(1,"1:2:3:____::8");
		ipv6test(1,"____:2::8");
		ipv6test(1,"1::____");
		ipv6test(1,"____::2:3:4:5:6:7");
		ipv6test(1,"____::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:____");
		ipv6test(1,"1::2:____:4");
		ipv6test(1,"1::____:3");
		ipv6test(1,"1::____");
		
		ipv6test(0,"::ffff:_____:0");
		ipv6test(0,"_____::1");
		ipv6test(0,"1:2:3:4:_____:6:7:8");
		ipv6test(0,"1:2:_____:4:5:6::8");
		ipv6test(0,"1:2:3:4:5::_____");
		ipv6test(0,"1:2:3:_____::8");
		ipv6test(0,"_____:2::8");
		ipv6test(0,"1::_____");
		ipv6test(0,"_____::2:3:4:5:6:7");
		ipv6test(0,"_____::2:3:4:5:6");
		ipv6test(0,"1::2:3:4:_____");
		ipv6test(0,"1::2:_____:4");
		ipv6test(0,"1::_____:3");
		ipv6test(0,"1::_____");
		
		ipv6test(0,"::ffff:ff___:0");
		ipv6test(0,"f____::1");
		ipv6test(0,"1:2:3:4:ffff_:6:7:8");
		ipv6test(0,"1:2:ffff_:4:5:6::8");
		ipv6test(0,"1:2:3:4:5::f_f__");
		ipv6test(0,"1:2:3:fff__::8");
		ipv6test(0,"f___f:2::8");
		ipv6test(0,"1::ff_ff");
		ipv6test(0,"ff_ff::2:3:4:5:6:7");
		ipv6test(0,"f____::2:3:4:5:6");
		ipv6test(0,"1::2:3:4:F____");
		ipv6test(0,"1::2:FF___:4");
		ipv6test(0,"1::FFF__:3");
		ipv6test(0,"1::FFFF_");
		
		ipv6test(0,"::ffff:_2_:0");
		ipv6test(0,"_2_::1");
		ipv6test(0,"1:2:3:4:_2_:6:7:8");
		ipv6test(0,"1:2:_2_:4:5:6::8");
		ipv6test(0,"1:2:3:4:5::_2_");
		ipv6test(0,"1:2:3:_2_::8");
		ipv6test(0,"_2_:2::8");
		ipv6test(0,"1::_2_");
		ipv6test(0,"_2_::2:3:4:5:6:7");
		ipv6test(0,"_2_::2:3:4:5:6");
		ipv6test(0,"1::2:3:4:_2_");
		ipv6test(0,"1::2:_2_:4");
		ipv6test(0,"1::_2_:3");
		ipv6test(0,"1::_2_");
		
		ipv6test(0,"::ffff:_2:0");
		ipv6test(0,"_2::1");
		ipv6test(0,"1:2:3:4:_2:6:7:8");
		ipv6test(0,"1:2:_2:4:5:6::8");
		ipv6test(0,"1:2:3:4:5::_2");
		ipv6test(0,"1:2:3:_2::8");
		ipv6test(0,"_2:2::8");
		ipv6test(0,"1::_2");
		ipv6test(0,"_2::2:3:4:5:6:7");
		ipv6test(0,"_2::2:3:4:5:6");
		ipv6test(0,"1::2:3:4:_2");
		ipv6test(0,"1::2:_2:4");
		ipv6test(0,"1::_2:3");
		ipv6test(0,"1::_2");
		
		ipv6test(1,"::ffff:2_:0");
		ipv6test(1,"2_::1");
		ipv6test(1,"1:2:3:4:2_:6:7:8");
		ipv6test(1,"1:2:2_:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::2_");
		ipv6test(1,"1:2:3:2_::8");
		ipv6test(1,"2_:2::8");
		ipv6test(1,"1::2_");
		ipv6test(1,"2_::2:3:4:5:6:7");
		ipv6test(1,"2_::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:2_");
		ipv6test(1,"1::2:2_:4");
		ipv6test(1,"1::2_:3");
		ipv6test(1,"1::2_");
		
		ipv6test(1,"::ffff:2___:0");
		ipv6test(1,"2___::1");
		ipv6test(1,"1:2:3:4:2___:6:7:8");
		ipv6test(1,"1:2:2___:4:5:6::8");
		ipv6test(1,"1:2:3:4:5::2___");
		ipv6test(1,"1:2:3:2___::8");
		ipv6test(1,"2___:2::8");
		ipv6test(1,"1::2___");
		ipv6test(1,"2___::2:3:4:5:6:7");
		ipv6test(1,"2___::2:3:4:5:6");
		ipv6test(1,"1::2:3:4:2___");
		ipv6test(1,"1::2:2___:4");
		ipv6test(1,"1::2___:3");
		ipv6test(1,"1::2___");
		
		ipv6test(1,"::fff_:2___:0");
		ipv6test(1,"2___::_");
		ipv6test(1,"1:2:3:4:2___:6_:7_:8");
		ipv6test(1,"1:2:2___:4:5:6::8__");
		ipv6test(1,"1:2:3_:4:5::2___");
		ipv6test(1,"1:2:3:2___::8");
		ipv6test(1,"2___:2::8");
		ipv6test(1,"1::2___");
		ipv6test(1,"2___::2_:3__:4:5:6:7");
		ipv6test(1,"2___::2:3_:4:5:6");
		ipv6test(1,"1::2:3:4_:2___");
		ipv6test(1,"1::2:2___:4f__");
		ipv6test(1,"1___::2___:3___");
		ipv6test(1,"1_::2___");
		
		ipv6test(isLenient(), "*:1:1._.__");
		ipv6test(1, "*:1:1._.__.___");
		//ipv6test(0, "*:_:1:_.1.1._");//this passes validation but conversion to mask fails because the ipv4 ranges cannot be converted to ipv6 ranges
		ipv6test(1, "*:_:1:1._.1._");
		ipv6test(1, "*:_:1:_.___.1._");
		ipv6test(1, "*:_:1:_.___._.___");
		ipv6test(1, "1:*:1_:1:1.1_.1.1");
		
		ipv6test(0, "1:1:1.2_.1");
		ipv6test(0, "1:1:1.2__.1.1");
		ipv6test(0, "1:1:_.*");
		ipv6test(0, "1:1:1._");
		
		super.runTest();
	}
	
}