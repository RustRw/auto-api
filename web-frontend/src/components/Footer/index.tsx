import { GithubOutlined } from '@ant-design/icons';
import { DefaultFooter } from '@ant-design/pro-components';
import React from 'react';

const Footer: React.FC = () => {
  return (
    <DefaultFooter
      style={{
        background: 'none',
      }}
      copyright="2024 Auto API Team"
      links={[
        {
          key: 'Auto API Platform',
          title: 'Auto API Platform',
          href: '#',
          blankTarget: true,
        },
        {
          key: 'github',
          title: <GithubOutlined />,
          href: '#',
          blankTarget: true,
        },
        {
          key: 'Auto API',
          title: 'Auto API',
          href: '#',
          blankTarget: true,
        },
      ]}
    />
  );
};

export default Footer;