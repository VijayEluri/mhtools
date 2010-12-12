/*  MHP2GDEC v1.0 - MH data.bin/xxxx.bin decrypter
    Copyright (C) 2008 Codestation

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package crypt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Decrypter extends DecryptTable {
	
	public void decrypt_index(String in, ByteArrayOutputStream index_buffer) {
		try {
			RandomAccessFile filein = new RandomAccessFile(in, "r");
			RandomAccessFile fileout = new RandomAccessFile("index.bin", "rw");
			fileout.setLength(0);
			byte[] buffer = new byte[4];
			System.out.println("Decrypting index...");
			initSeed(0);
			boolean table_end = false;
			boolean end_flag = false;
			int i = 0;
			while(!table_end) {
				filein.read(buffer);
				buffer[0] = decrypt_table[buffer[0] & 0xFF];
				buffer[1] = decrypt_table[buffer[1] & 0xFF];
				buffer[2] = decrypt_table[buffer[2] & 0xFF];
				buffer[3] = decrypt_table[buffer[3] & 0xFF];
				long beta = getBeta();
				long alpha = get_table_value(buffer, 0);
				long gamma = alpha ^ beta;
				
				if(gamma > 0xFF) {
					end_flag = true;
				} else  if(end_flag) {
					table_end = true;
					continue;
				}
				set_table_value(buffer, 0, gamma);
				fileout.write(buffer);
				if(index_buffer != null)
					index_buffer.write(buffer);
				i += 4;
			}
			fileout.close();
			System.out.println("Index size: " + i + " bytes");
			System.out.println("File count: " + i / 4);	
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void decrypt_whole(String in, String out) {
		ByteArrayOutputStream index_buffer = new ByteArrayOutputStream();
		decrypt_index(in, index_buffer);
		RandomAccessFile filein;
		try {
			filein = new RandomAccessFile(in, "r");
			byte index_table[] = index_buffer.toByteArray();
			int files_count = index_table.length / 4;
			new File(out).mkdir();
			boolean create_subdirectory = true;
			int last_subdirectory = 0;
			long last_offset = 0;
			for(int i = 0; i < files_count; i++) {
				if(create_subdirectory) {
					last_subdirectory = i / 1000;
					new File(out + "/0" + Integer.toString(last_subdirectory)).mkdir();
					create_subdirectory = false;
				} else {
					if(last_subdirectory < i / 1000) {
						create_subdirectory = true;
					}
				}				
				long offset = last_offset;
				last_offset = get_table_value(index_table, i * 4);
				long file_length = (get_table_value(index_table, i * 4) - offset) << 11;
				String fileout = out +
					"/0" + Integer.toString(last_subdirectory) +
					"/" + String.format("%04d.bin", i);
				System.out.print("Decrypting "  + fileout + "(" + file_length + " bytes/offset: " + (offset << 11) + ") ... ");
				decrypt_internal(filein, offset, file_length, fileout, false);
									
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private void decrypt_internal(RandomAccessFile filein, long pos, long size, String out, boolean single) {
		try {
			if(!single)
				filein.seek(pos << 11);
			RandomAccessFile fileout = new RandomAccessFile(out, "rw");
			byte buffer[] = new byte[1024];
			fileout.setLength(0);
			initSeed(pos);	
			while (size > 0) {
				int read = filein.read(buffer);
				size -= read;
				for(int i = 0; i < read; i += 4) {
					buffer[i] = decrypt_table[buffer[i] & 0xFF];
					buffer[i+1] = decrypt_table[buffer[i+1] & 0xFF];
					buffer[i+2] = decrypt_table[buffer[i+2] & 0xFF];
					buffer[i+3] = decrypt_table[buffer[i+3] & 0xFF];
					long alpha = get_table_value(buffer, i);
					long beta = getBeta();
					long gamma = alpha ^ beta;
					set_table_value(buffer, i, gamma);
				}
				fileout.write(buffer);
			}
			fileout.close();
			System.out.println("Finished!");
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void decrypt(String in, String out) {
		RandomAccessFile filein = null;
		try {
			filein = new RandomAccessFile(in, "r");
			System.out.print("Decrypting "  + out + " ... ");
			decrypt_internal(filein, getOffset(extractNumber(in)), filein.length(), out, true);
			
		}catch(FileNotFoundException e) {
			e.printStackTrace();
		}catch(IOException e) {
			e.printStackTrace();
		} finally {
			if(filein != null)
				try {
					filein.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}
