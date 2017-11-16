package org.onetwo.common.db.sqlext;

import org.onetwo.dbm.dialet.DBDialect.LockInfo;


public class DefaultExtQueryDialetImpl implements ExtQueryDialet {
	
	public static final char[] REPLACE_CHARS = new char[]{
		'.', ',', '(', ')', '+', '-', '*', '/'
	};

	public DefaultExtQueryDialetImpl() {
	}

	@Override
	public String getNamedPlaceHolder(String name, int position) {
		if(name.indexOf(' ')!=-1)
			name = name.replaceAll("[\\s']", "");
		String newName = new StringBuilder(name).append(position).toString();
//		newName = newName.replaceAll("[\\.\\(\\),]", "_");
		for(char ch : REPLACE_CHARS){
			if(newName.indexOf(ch)!=-1){
				newName = newName.replace(ch, '_');
			}
		}
		return newName.toString();
	}

	@Override
	public String getPlaceHolder(int position) {
		return "?";
	}
	
	public String getNullsOrderby(String nullsOrder){
		return nullsOrder;
	}

	@Override
	public String getLockSqlString(LockInfo lock) {
		throw new UnsupportedOperationException();
	}

}
