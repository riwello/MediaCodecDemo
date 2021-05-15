//
//
//  H264ToJPEG.h
//
//  Created by censt on 2021/5/12.
//  Copyright © 2021 cnest. All rights reserved.
//

#ifndef H264ToJPEG_h
#define H264ToJPEG_h


//解码
// data 原数据  len 原数据长度
//rData  解码后的图片数据  rLen 长度
int decodeData(unsigned char* data,int len,unsigned char **rData,int *rLen);



#endif /* H264ToJPEG_h */
