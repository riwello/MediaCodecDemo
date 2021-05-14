//
//  SNDecodeHelper.h
//  Video
//
//  Created by censt on 2021/5/12.
//  Copyright © 2021 cnest. All rights reserved.
//

#ifndef SNDecodeHelper_h
#define SNDecodeHelper_h


//解码
// data 原数据  len 原数据长度
//rData  解码后的图片数据  rLen 长度
void decodeData(unsigned char* data,int len,unsigned char **rData,int *rLen);

//销毁解码器
void destryDecoder(void);


#endif /* SNDecodeHelper_h */
